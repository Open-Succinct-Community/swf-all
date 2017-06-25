package com.venky.swf.plugins.background.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;

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
		synchronized (queue) {
			if (!keepAlive()) {
				throw new ShutdownInitiatedException();
			}
			queue.addAll(tasks);
			queue.notifyAll();
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

	public Task waitUntilNextTask() {
		Task dt = null;
		synchronized (queue) {
			while (!evicted() && keepAlive() && queue.isEmpty() ) {
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
			if (!evicted() && keepAlive() && !queue.isEmpty() ) {
				dt = queue.poll();
				Config.instance().getLogger(getClass().getName())
				.finest("Number of Tasks remaining in Queue pending workers:" + queue.size());
				queue.notifyAll();
			}
		}
		return dt;
	}
	
	public Task next() {
		decrementWorkerCount();
		Task task =  waitUntilNextTask();
		if (task != null) {
			incrementWorkerCount();
		}
		return task;
	}

	private Bucket numWorkersWorking = new Bucket();

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
		if (!persist) {
			List<Task> taskHolders = new LinkedList<Task>();
			for (Task task : tasks){
				taskHolders.add(new TaskHolder(task));
			}
			addAll(taskHolders);
		}else {
			for (Task task: tasks){ 
				try {
					DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
					ByteArrayOutputStream os = new ByteArrayOutputStream(); 
					ObjectOutputStream oos = new ObjectOutputStream(os);
					oos.writeObject(task);
					de.setPriority(task.getTaskPriority().getValue());
					de.setData(new ByteArrayInputStream(os.toByteArray()));
					de.save();
				} catch (IOException ex) {
					throw new RuntimeException(task.getClass().getName() ,ex);
				}
			}
			Database.getInstance().getCurrentTransaction().setAttribute(PersistedTaskPollingAgent.class.getName()+".trigger", true);
		}
	}
}
