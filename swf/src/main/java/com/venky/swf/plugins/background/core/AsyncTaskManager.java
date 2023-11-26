package com.venky.swf.plugins.background.core;

import com.venky.cache.Cache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.model.io.json.JSONModelReader;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.plugins.background.extensions.InMemoryTaskQueueManager;
import com.venky.swf.routing.Config;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class AsyncTaskManager  {

	public AsyncTaskManager() {
		queue();
		for (int i = 0; i < getInitialNumWorkerThreads(); i++) {
			addWorker();
		}
	}

	public void addWorker(int incrementBy){
		for (int i = 0 ; i< incrementBy ; i ++){
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

	final Bucket numWorkersToEvict = new Bucket();
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

	public final int getInitialNumWorkerThreads() {
		return Config.instance().getIntProperty( String.format("swf.plugins.background.core.%s.workers.numThreads", getClass().getSimpleName()) , Config.instance().getIntProperty("swf.plugins.background.core.workers.numThreads", 1));
	}

	private AtomicInteger instanceNumber = new AtomicInteger();
	public AsyncTaskWorker createWorker() {
		return new AsyncTaskWorker(this, instanceNumber.incrementAndGet());
	}

	private List<AsyncTaskWorker> workerThreads = new Vector<>();
	public int getNumWorkers(){
		return workerThreads.size();
	}

	private Queue<CoreTask> queue = null;

	protected Queue<CoreTask> queue() {
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

	void evict(AsyncTaskWorker asyncTaskWorker) {
		if (Thread.currentThread() == asyncTaskWorker) {
			//Defense.
			workerThreads.remove(asyncTaskWorker);
		}
	}

	public static class ShutdownInitiatedException extends RuntimeException {
		private static final long serialVersionUID = -8216421138960049897L;
	}
	public void addAll(Collection<? extends CoreTask> tasks) {
		Cache<Boolean,List<CoreTask>> remoteableTasks = new Cache<Boolean, List<CoreTask>>() {
			@Override
			protected List<CoreTask> getValue(Boolean aBoolean) {
				return new ArrayList<>();
			}
		};
		for (CoreTask task :tasks){
			remoteableTasks.get(task.canExecuteRemotely()).add(task);
		}
		addAll(remoteableTasks.get(true),true);
		addAll(remoteableTasks.get(false),false);
	}
	private void addAll(Collection<CoreTask> tasks, boolean canExecuteRemote) {
		if (tasks.isEmpty()) {
			return;
		}
		if (!keepAlive()) {
			throw new ShutdownInitiatedException();
		}
		if (ObjectUtil.isVoid(getQueueServerURL()) || !canExecuteRemote){
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
			SWFHttpResponse response = new JSONModelReader<SWFHttpResponse>(SWFHttpResponse.class).read((JSONObject)jsonObject.get(ModelReflector.instance(SWFHttpResponse.class).getModelClass().getSimpleName()));

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
		synchronized (queue) {
			shutdown = true;
			queue.notifyAll();
		}
	}

	public CoreTask waitUntilNextTask(boolean localWorker, boolean waitForTask) {
		CoreTask dt = null;
		synchronized (queue){
			while (waitForTask && isWorkerAlive(localWorker) && queue.isEmpty() ){
				pullRemoteTasks(Config.instance().getIntProperty("swf.plugins.background.queue.server.batch",1000));
				if (queue.isEmpty()){
					try {
						Config.instance().getLogger(getClass().getName())
								.finest("Worker: going back to sleep as there is no work to be done.");
						//queue.wait(30*1000);
						queue.wait(30*1000);//Wait for 30 seconds on the queue
					} catch (InterruptedException ex) {
						Config.instance().getLogger(getClass().getName()).finest("Worker: waking up to look for work.");
					}
				}
			}
			if (isWorkerAlive(localWorker) && !queue.isEmpty() ) {
				if (localWorker || queue.peek().canExecuteRemotely()){
					dt = queue.poll();
					queue.notifyAll();
				}
				Config.instance().getLogger(getClass().getName())
						.finest(String.format("Number of Tasks remaining in %s Queue : %d" , getClass().getSimpleName() , queue.size()));
			}
		}

		return dt;
	}
	private boolean isWorkerAlive(boolean local){
		return (!local || !evicted()) && keepAlive();
	}

	private void pullRemoteTasks(int batch){
		if (ObjectUtil.isVoid(getQueueServerURL())){
			return ;
		}

		List<CoreTask> tasks = new ArrayList<>();
		JSONObject parameters = new JSONObject();
		parameters.put("BatchSize",batch);

		HashMap<String,String> headers = new HashMap<>();
		headers.put("content-type", MimeType.APPLICATION_JSON.toString());
		if (!ObjectUtil.isVoid(getApiKey())){
			headers.put("ApiKey",getApiKey());
		}
		int timeOut = 10000;
		try {
			InputStream stream = new Call<JSONAware>().url(getQueueServerURL() + "/next").headers(headers).timeOut(timeOut).method(HttpMethod.POST).
					inputFormat(InputFormat.JSON).input(parameters).getResponseStream();

			if (stream.available() > 0 ){
				SerializationHelper helper = new SerializationHelper();
				tasks = helper.read(stream);
			}

		}catch (Exception ex){
			Config.instance().getLogger(getClass().getName())
					.log(Level.WARNING,"Exception in finding tasks",ex);

		}

		addAll(tasks, false); //I Ihave pulled from remote.
	}



	public String getQueueServerURL(){
		return Config.instance().getProperty("swf.plugins.background.queue.server.url");
	}

	public String getApiKey() {
		return Config.instance().getProperty("swf.plugins.background.queue.server.apikey");
	}

	public CoreTask next() {
		return next(true,true);
	}
	public CoreTask next(boolean local,boolean wait) {
		if (local){
			decrementWorkerCount();
		}
		CoreTask task =  waitUntilNextTask(local,wait);
		if (task != null && local) {
			incrementWorkerCount();
		}
		return task;
	}

	private final Bucket numWorkersWorking = new Bucket();

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
					.info(String.format("Number of %s workers working %d" , getClass().getSimpleName() ,numWorkersWorking.intValue()));
		}
	}

	public void incrementWorkerCount() {
		synchronized (numWorkersWorking) {
			numWorkersWorking.increment();
			numWorkersWorking.notifyAll();
			Config.instance().getLogger(getClass().getName())
					.info(String.format("Number of %s workers working %d" , getClass().getSimpleName() ,numWorkersWorking.intValue()));
		}
	}

	public void waitUntilWorkersFinish() {
		synchronized (numWorkersWorking) {
			while (numWorkersWorking.intValue() != 0) {
				try {
					Config.instance().getLogger(getClass().getName())
							.info(String.format("Number of %s workers working %d" , getClass().getSimpleName() ,numWorkersWorking.intValue()));
					numWorkersWorking.wait();
				} catch (InterruptedException ex) {
					//do nothing
				}
			}
		}
	}
	public void waitUntilWorkersAreEvicted() {
		synchronized (numWorkersToEvict) {
			while (numWorkersToEvict.intValue() > 0){
				try {
					numWorkersToEvict.wait(5000);
				} catch (InterruptedException e) {
					//do nothing
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

	public <T extends CoreTask>  void execute(Collection<T> tasks){
		execute(tasks, false);
	}
	
	public <T extends CoreTask> void execute(Collection<T> tasks, boolean persist){
		if (getInitialNumWorkerThreads() == 0) {
			for (CoreTask task:tasks) {
				task.execute();
			}
		} else {
			pushAsyncTasks(tasks, persist);
		}
	}
	protected <T extends CoreTask> void pushAsyncTasks(Collection<T> tasks, boolean persist) {
		if (tasks.isEmpty()) {
			return;
		}
		if (!persist) {
            List<CoreTask> taskHolders = new LinkedList<CoreTask>();
            for (CoreTask task : tasks){
                taskHolders.add(new TaskHolder(task));
            }
            //addAll(taskHolders);
            InMemoryTaskQueueManager.getPendingTasks().addAll(taskHolders); //To ensures tasks are executed after commit.
		}else {
			SerializationHelper helper = new SerializationHelper();
			for (CoreTask task: tasks){
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
