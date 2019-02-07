package com.venky.swf.plugins.background.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.io.json.JSONModelReader;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.integration.JSON;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.AgentSeederTask;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent.PersistedTaskPoller;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.plugins.background.extensions.InMemoryTaskQueueManager;
import com.venky.swf.routing.Config;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

public class AsyncTaskManager  {
	
	private static AsyncTaskManager tm  = null;
	public static AsyncTaskManager getInstance() {
		if (tm == null) {
			synchronized (AsyncTaskManager.class) {
				if (tm == null){
					tm = new AsyncTaskManager();
				}
			}
		}
		return tm;
	}

	protected AsyncTaskManager() {
		this(getInitialNumWorkerThreads());
	}
	
	protected AsyncTaskManager(int numWorkers) {
		queue();
		for (int i = 0; i < numWorkers; i++) {
			addWorker();
		}
	}
	
	public void addWorker(){
		AsyncTaskWorker worker = null;
		incrementWorkerCount();
		worker = createWorker();
		workerThreads.add(worker);
		worker.start();
	}
	public void evictWorker(int number){
		synchronized (numWorkersToEvict) {
			numWorkersToEvict.increment(number);
			if (workerThreads.size() < numWorkersToEvict.intValue()) {
				int diff = numWorkersToEvict.intValue() - workerThreads.size();
				numWorkersToEvict.decrement(diff);
			}
			numWorkersToEvict.notifyAll();
		}
		wakeUp();
	}

	Bucket numWorkersToEvict = new Bucket();
	public boolean evicted(){
		synchronized (numWorkersToEvict) {
			if (numWorkersToEvict.intValue() > 0 && Thread.currentThread() instanceof AsyncTaskWorker){
				if (workerThreads.remove(Thread.currentThread())){
					numWorkersToEvict.decrement();
				}
				numWorkersToEvict.notifyAll();
				return true;
			}
			return false;
		}
	}

	public static final int getInitialNumWorkerThreads() {
		return Config.instance().getIntProperty("swf.plugins.background.core.workers.numThreads", 1);
	}

	private AtomicInteger instanceNumber = new AtomicInteger();
	public AsyncTaskWorker createWorker() {
		return new AsyncTaskWorker(this, instanceNumber.incrementAndGet());
	}

	private List<AsyncTaskWorker> workerThreads = new Vector<>();
	public int getNumWorkers(){
		return workerThreads.size();
	}

	private Queue<Task> queue = null;

	protected Queue<Task> queue() {
		if (queue != null) {
			return queue;
		}
		synchronized (this) {
			if (queue == null) {
				queue = new WeightedPriorityQueue();
			}
		}
		return queue;
	}

	public static class ShutdownInitiatedException extends RuntimeException {
		private static final long serialVersionUID = -8216421138960049897L;
	}

	public void addAll(Collection<Task> tasks) {
		if (tasks.isEmpty()) {
			return;
		}
		if (!keepAlive()) {
			throw new ShutdownInitiatedException();
		}
		if (ObjectUtil.isVoid(getQueueServerURL())){
			synchronized (queue) {
				queue.addAll(tasks);
				queue.notifyAll();
			}
		}else {
			HashMap<String,String> headers = new HashMap<>();
			headers.put("content-type", MimeType.APPLICATION_JSON.toString());
			if (!ObjectUtil.isVoid(getApiKey())){
				headers.put("ApiKey",getApiKey());
			}
			int timeOut = 0 ;
			SeekableByteArrayOutputStream os = new SeekableByteArrayOutputStream();
			new SerializationHelper().write(os,tasks);

			JSONObject jsonObject = new Call<Object>().url(getQueueServerURL()+"/push").headers(headers).timeOut(timeOut).
					method(HttpMethod.POST).inputFormat(InputFormat.INPUT_STREAM).input(os.toByteArray()).getResponseAsJson();
			SWFHttpResponse response = new JSONModelReader<SWFHttpResponse>(SWFHttpResponse.class).read(jsonObject);

			if (!ObjectUtil.equals(response.getStatus(),"OK")){
				throw new RuntimeException("Unable to push Tasks to the queue server.");
			}
		}
	}

	protected boolean keepAlive() {
		synchronized (queue) {
			if (shutdown) {
				return false;
			} else {
				return true;
			}
		}
	}

	private boolean shutdown = false;

	public void shutdown() {
		while (true) {
			Config.instance().getLogger(getClass().getName()).info("Waiting for all Threads to shutdown");
			evictWorker(workerThreads.size());
			waitUntilWorkersAreEvicted();
			break;
		}
		synchronized (queue) {
			shutdown = true;
			queue.notifyAll();
		}
	}

	public Task waitUntilNextTask(boolean local, boolean wait) {
		Task dt = null;
		if (ObjectUtil.isVoid(getQueueServerURL())){
			synchronized (queue) {
				while (wait && isWorkerAlive(local) && queue.isEmpty()) {
					try {
						Config.instance().getLogger(getClass().getName())
								.finest("Worker: going back to sleep as there is no work to be done.");
						//queue.wait(30*1000);
						queue.wait();
					} catch (InterruptedException ex) {
						Config.instance().getLogger(getClass().getName()).finest("Worker: waking up to look for work.");
						//
					}
				}
				if (isWorkerAlive(local) && !queue.isEmpty() ) {
					if (local) {
						dt = queue.poll();
					}else {
						for (Iterator<Task> i = queue.iterator(); i.hasNext() ; ){
							Task t = i.next();
							if (t.canExecuteRemotely()) {
								i.remove();
								dt = t;
								break;
							}
						}
					}
					Config.instance().getLogger(getClass().getName())
							.finest("Number of Tasks remaining in Queue pending workers:" + queue.size());
					queue.notifyAll();
				}
			}
		}else if (isWorkerAlive(local)){
			do{
				dt = getNextRemoteTask();

				if (dt == null && wait){
					Config.instance().getLogger(getClass().getName())
							.log(Level.INFO,"No Tasks Found");
					try {
						Thread.sleep(30 * 1000);
					}catch (InterruptedException ex){
						//
					}
				}
			}while (dt == null && wait && isWorkerAlive(local));
		}
		return dt;
	}
	private boolean isWorkerAlive(boolean local){
		return (!local || !evicted()) && keepAlive();
	}

	private Task getNextRemoteTask(){
		JSONObject parameters = new JSONObject();

		HashMap<String,String> headers = new HashMap<>();
		headers.put("content-type", MimeType.APPLICATION_JSON.toString());
		if (!ObjectUtil.isVoid(getApiKey())){
			headers.put("ApiKey",getApiKey());
		}
		int timeOut = 10000;
		Task dt = null;
		try {
			InputStream stream = new Call<JSONAware>().url(getQueueServerURL() + "/next").headers(headers).timeOut(timeOut).method(HttpMethod.POST).
					inputFormat(InputFormat.JSON).input(parameters).getResponseStream();

			if (stream.available() > 0 ){
				SerializationHelper helper = new SerializationHelper();
				dt = helper.read(stream);
			}

		}catch (Exception ex){
			Config.instance().getLogger(getClass().getName())
					.log(Level.WARNING,"Exception in finding tasks",ex);
			shutdown();
		}
		return dt;
	}
	public String getQueueServerURL(){
		return Config.instance().getProperty("swf.plugins.background.queue.server.url");
	}

	public String getApiKey() {
		return Config.instance().getProperty("swf.plugins.background.queue.server.apikey");
	}

	public Task next() {
		return next(true,true);
	}
	public Task next(boolean local,boolean wait) {
		if (local){
			decrementWorkerCount();
		}
		Task task =  waitUntilNextTask(local,wait);
		if (task != null && local) {
			incrementWorkerCount();
		}
		return task;
	}

	private Bucket numWorkersWorking = new Bucket();

	public int workerCount(){
		synchronized (numWorkersWorking){
			return numWorkersWorking.intValue();
		}
	}

	public void decrementWorkerCount() {
		synchronized (numWorkersWorking) {
			numWorkersWorking.decrement();
			numWorkersWorking.notifyAll();
			Config.instance().getLogger(getClass().getName())
					.info("Number of workers working " + numWorkersWorking.intValue());
		}
	}

	public void incrementWorkerCount() {
		synchronized (numWorkersWorking) {
			numWorkersWorking.increment();
			numWorkersWorking.notifyAll();
			Config.instance().getLogger(getClass().getName())
					.info("Number of workers working " + numWorkersWorking.intValue());
		}
	}

	public void waitUntilWorkersFinish() {
		synchronized (numWorkersWorking) {
			while (numWorkersWorking.intValue() != 0) {
				try {
					Config.instance().getLogger(getClass().getName())
							.info("Number of workers working " + numWorkersWorking.intValue());
					numWorkersWorking.wait();
				} catch (InterruptedException ex) {

				}
			}
		}
	}
	public void waitUntilWorkersAreEvicted() {
		synchronized (numWorkersToEvict) {
			while (numWorkersToEvict.intValue() != 0){
				try {
					numWorkersToEvict.wait(5000);
				} catch (InterruptedException e) {
					
				}
			}
		}
	}

	public void wakeUp() {
		synchronized (queue) {
			queue.notifyAll();
			Config.instance().getLogger(getClass().getName()).finest("Waking up Daemon.");
		}
	}
	
	public void waitIfQueueIsEmpty(int seconds){
		synchronized (queue) {
			if (queue.isEmpty() && keepAlive()) {
				try {
					queue.wait(seconds * 1000); 
				} catch (InterruptedException e) {
					//
				}
			}
		}
	}
	public void waitUntilQueueIsEmpty() {
		synchronized (queue) {
			int initialSize = queue.size();
			while (!queue.isEmpty()) {
				try {
					Config.instance().getLogger(getClass().getName())
							.finest("Daemon going back to sleep as there are pending tasks still to be completed.");
					queue.wait();
				} catch (InterruptedException ex) {
					Config.instance().getLogger(getClass().getName())
							.finest("Daemon woke up to check if all tasks are complete.");
				}
				int newSize = queue.size();
				if (newSize == initialSize) {
					// Why was my sleep distubed. May be I am to kill myself!!
					if (!keepAlive()) {
						break;
					}
				} else {
					initialSize = newSize;
				}
			}
		}
	}

	public <T extends Task>  void execute(Collection<T> tasks){ 
		execute(tasks, false);
	}
	
	public <T extends Task> void execute(Collection<T> tasks, boolean persist){ 
		if (getInitialNumWorkerThreads() == 0) {
			for (Task task:tasks) {
				task.execute();
			}
		} else {
			pushAsyncTasks(tasks, persist);
		}
	}
	protected <T extends Task> void pushAsyncTasks(Collection<T> tasks, boolean persist) {
		if (tasks.isEmpty()) {
			return;
		}
		if (!persist) {
            List<Task> taskHolders = new LinkedList<Task>();
            for (Task task : tasks){
                taskHolders.add(new TaskHolder(task));
            }
            //addAll(taskHolders);
            InMemoryTaskQueueManager.getPendingTasks().addAll(taskHolders); //To ensures tasks are executed after commit.
		}else {
			SerializationHelper helper = new SerializationHelper();
			for (Task task: tasks){ 
				DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				helper.write(os,task);
				//ObjectOutputStream oos = new ObjectOutputStream(os);
				de.setPriority(task.getTaskPriority().getValue());
				de.setData(new ByteArrayInputStream(os.toByteArray()));
				de.save();
			}
			//Database.getInstance().getCurrentTransaction().setAttribute(PersistedTaskPollingAgent.class.getName()+".trigger", true);
			//Trigger PersistedTaskPollingAgent through Cron. KEep loosely coupled.
		}
	}
}
