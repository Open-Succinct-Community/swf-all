package com.venky.swf.plugins.background.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.venky.core.util.Bucket;
import com.venky.swf.plugins.background.core.Task.Priority;
import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;

public abstract class AsyncTaskManager<T extends Task & Comparable<? super T>> {
	@SuppressWarnings("rawtypes")
	private static Map<Class, AsyncTaskManager> _instance = new HashMap<Class,AsyncTaskManager>();

	@SuppressWarnings("unchecked")
	public static <T extends Task & Comparable<? super T>> AsyncTaskManager<T> getInstance(Class<T> taskClass) {
		AsyncTaskManager<T> tm = _instance.get(taskClass);
		if (tm == null) {
			synchronized (_instance) {
				tm = _instance.get(taskClass);
				if (tm == null) {
					tm = buldTaskManager(taskClass);
					_instance.put(taskClass, tm);
				}
			}
		}
		return tm;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Task & Comparable<? super T>> AsyncTaskManager<T> buldTaskManager(Class<T> taskClass) {
		if (DelayedTask.class.isAssignableFrom(taskClass)) {
			return (AsyncTaskManager<T>) new DelayedTaskManager();
		} else if (TaskHolder.class.isAssignableFrom(taskClass)) {
			return (AsyncTaskManager<T>) new AsyncTaskManager<TaskHolder>(){
				
				@Override
				protected void pushAsyncTasks(Collection<Task> tasks, Priority priority) {
					List<TaskHolder> taskHolders = new LinkedList<TaskHolder>();
					for (Task task : tasks){
						taskHolders.add(new TaskHolder(task, priority));
					}
					addAll(taskHolders);
				}
			};	
		} else {
			throw new RuntimeException("Don't know how tp build TaskManager for " + taskClass.getName());
		}
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
		AsyncTaskWorker<T> worker = null;
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
			if (numWorkersToEvict.intValue() > 0 && Thread.currentThread() instanceof AsyncTaskWorker<?>){
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
	public AsyncTaskWorker<T> createWorker() {
		return new AsyncTaskWorker<T>(this, instanceNumber.incrementAndGet());
	}

	private List<AsyncTaskWorker<T>> workerThreads = new Vector<>();
	public int getNumWorkers(){
		return workerThreads.size();
	}

	private Queue<T> queue = null;

	protected Queue<T> queue() {
		if (queue != null) {
			return queue;
		}
		synchronized (this) {
			if (queue == null)
				queue = new PriorityQueue<T>();
		}
		return queue;
	}

	public static class ShutdownInitiatedException extends RuntimeException {
		private static final long serialVersionUID = -8216421138960049897L;
	}

	public void addAll(Collection<T> tasks) {
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

	public T waitUntilNextTask() {
		T dt = null;
		synchronized (queue) {
			while (!evicted() && keepAlive() && queue.isEmpty() ) {
				try {
					Config.instance().getLogger(getClass().getName())
							.finest("Worker: going back to sleep as there is no work to be done.");
					queue.wait(30*1000);
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
	
	public T next() {
		decrementWorkerCount();
		T task =  waitUntilNextTask();
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

	public void execute(Task task, Priority priority) {
		execute(Arrays.asList(task),priority);
	}
	public void execute(Collection<Task> tasks, Priority priority) {
		if (getInitialNumWorkerThreads() == 0) {
			for (Task task:tasks) {
				task.execute();
			}
		} else {
			pushAsyncTasks(tasks, priority);
		}
	}
	
	protected abstract void pushAsyncTasks(Collection<Task> task, Priority priority) ;
	

	public static void shutdownAll() {
		AsyncTaskManager.getInstance(TaskHolder.class).shutdown();
		AsyncTaskManager.getInstance(DelayedTask.class).shutdown();
	}

	public static void wakeUpAll() {
		AsyncTaskManager.getInstance(TaskHolder.class).wakeUp();
		AsyncTaskManager.getInstance(DelayedTask.class).wakeUp();
	}

}
