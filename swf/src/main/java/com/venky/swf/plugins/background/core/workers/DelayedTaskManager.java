package com.venky.swf.plugins.background.core.workers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.PriorityQueue;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.plugins.background.db.model.DelayedTask.Priority;
import com.venky.swf.routing.Config;

public class DelayedTaskManager {
	private PriorityQueue<DelayedTask> queue = new PriorityQueue<DelayedTask>();
	private final DelayedTaskPollingThread dtpt ;
	private final DelayedTaskWorker[] workers;
	private static DelayedTaskManager _instance = null;
	public static DelayedTaskManager instance(){
		if (_instance == null){
			synchronized (DelayedTaskManager.class) {
				if (_instance == null){
					_instance = new DelayedTaskManager();
				}
			}
		}
		return _instance;
	}
	public  int getNumWorkerThreads(){
		return workers.length;
	}
	
	private DelayedTaskManager(){
		this(Config.instance().getIntProperty("swf.plugins.background.core.workers.numThreads", 1));
	}
	
	private Bucket numWorkersWorking = new Bucket();
	private Bucket numPollingThreadsWorking = new Bucket();
	
	private DelayedTaskManager(int numWorkerThreads){
		super();
		workers = new DelayedTaskWorker[numWorkerThreads];
		for (int i = 0 ; i < workers.length; i ++){
			incrementWorkerCount();
			workers[i] = new DelayedTaskWorker(this,i);
			workers[i].start();
		}
		incrementPollerCount();
		dtpt = new DelayedTaskPollingThread(this);
		dtpt.start();
	}
	
	public void wakeUp(){
		waitTillPollersFinish();
		synchronized (queue) {
			queue.notifyAll();
			Config.instance().getLogger(getClass().getName()).finest("Waking up Daemon.");
		}
	}
	
	public static class ShutdownInitiatedException extends RuntimeException {
		private static final long serialVersionUID = -8216421138960049897L;
	}
	
	public void addDelayedTasks(Collection<DelayedTask> tasks){
		if (tasks.isEmpty() ){
			return;
		}
		synchronized (queue) {
			if (!keepAlive()){
				throw new ShutdownInitiatedException();
			}
			queue.addAll(tasks);
			queue.notifyAll();
		}
	}

	private boolean keepAlive(){
		synchronized (queue) {
			if (shutdown){
				return false;
			}else {
				return true;
			}
		}
	}

	public DelayedTask next(){
		decrementWorkerCount();
		DelayedTask dt = waitUntilNextTask();
		if (dt != null) {
			incrementWorkerCount();
		}
		return dt;
	}
	
	public boolean needMoreTasks(){
		synchronized (queue) {
			decrementPollerCount();
			if (queue.isEmpty()) {
				//No Tasks Found!! Simply Sleep.
				try {
					queue.wait(2*60*1000); // Max of 2 minutes.
				} catch (InterruptedException e) {
					//
				}
			}
		}
		waitIfQueueIsNotEmpty();
		waitTillWorkersFinish(); // Avoid Locking with Workers.
		boolean keepAlive = keepAlive();
		if (keepAlive) {
			incrementPollerCount();
		}
		return keepAlive;
	}

	public DelayedTask waitUntilNextTask(){
		DelayedTask dt = null ;
		synchronized (queue) {
			while (queue.isEmpty() && keepAlive()){
				try {
					Config.instance().getLogger(getClass().getName()).finest("Worker: going back to sleep as there is no work to be done.");
					queue.wait();
				}catch (InterruptedException ex){
					Config.instance().getLogger(getClass().getName()).finest("Worker: waking up to look for work.");
					//
				}
			}
			if (keepAlive() && !queue.isEmpty()){
				dt = queue.poll();
				queue.notifyAll();
			}
		}
		return dt;
	}
	public void decrementWorkerCount(){
		synchronized (numWorkersWorking) {
			numWorkersWorking.decrement();
			numWorkersWorking.notifyAll();
			Config.instance().getLogger(getClass().getName()).info("Number of workers working "  + numWorkersWorking.intValue());
		}
		wakeUp();
	}
	public void incrementWorkerCount(){
		synchronized (numWorkersWorking) {
			numWorkersWorking.increment();
			numWorkersWorking.notifyAll();
			Config.instance().getLogger(getClass().getName()).info("Number of workers working "  + numWorkersWorking.intValue());
		}
	}
	public void waitTillWorkersFinish(){
		synchronized (numWorkersWorking) {
			while (numWorkersWorking.intValue() != 0){
				try {
					Config.instance().getLogger(getClass().getName()).info("Number of workers working "  + numWorkersWorking.intValue());
					numWorkersWorking.wait();
				}catch(InterruptedException ex){
					
				}
			}
		}
	}
	
	public void decrementPollerCount(){
		synchronized (numPollingThreadsWorking) {
			numPollingThreadsWorking.decrement();
			numPollingThreadsWorking.notifyAll();
			Config.instance().getLogger(getClass().getName()).info("Number of pollers working "  + numPollingThreadsWorking.intValue());
		}
	}
	public void incrementPollerCount(){
		synchronized (numPollingThreadsWorking) {
			numPollingThreadsWorking.increment();
			numPollingThreadsWorking.notifyAll();
			Config.instance().getLogger(getClass().getName()).info("Number of pollers working "  + numPollingThreadsWorking.intValue());
		}
	}
	
	public void waitTillPollersFinish(){
		synchronized (numPollingThreadsWorking) {
			while (numPollingThreadsWorking.intValue() != 0){
				try {
					Config.instance().getLogger(getClass().getName()).info("Number of pollers working "  + numPollingThreadsWorking.intValue());
					numPollingThreadsWorking.wait();
				}catch(InterruptedException ex){
					
				}
			}
		}
		
	}
	public void waitIfQueueIsNotEmpty(){
		synchronized (queue) {
			int initialSize = queue.size();
			while (!queue.isEmpty()){
				try {
					Config.instance().getLogger(getClass().getName()).finest("Daemon going back to sleep as there are pending tasks still to be completed.");
					queue.wait();
				}catch (InterruptedException ex){
					Config.instance().getLogger(getClass().getName()).finest("Daemon woke up to check if all tasks are complete.");
				}
				int newSize = queue.size();
				if (newSize == initialSize){
					//Why was my sleep distubed. May be I am to kill myself!!
					if(!keepAlive()){
						break;
					}
				}else {
					initialSize = newSize;
				}
			}
		}
	}
	
	private boolean shutdown =  false; 
	public void shutdown(){
		synchronized (queue) {
			shutdown = true;
			queue.notifyAll();
		}
		while (true){
			Config.instance().getLogger(getClass().getName()).info("Waiting for all Threads to shutdown");
			try {
				dtpt.join();
				Config.instance().getLogger(getClass().getName()).info("Polling Thread has shutdown");
				for (int i = 0 ; i < workers.length ; i ++ ){
					workers[i].join();
					Config.instance().getLogger(getClass().getName()).info("Worker " + i  +  " of " + workers.length + " has shutdown");
				}
				break;
			} catch (InterruptedException e) {
				//
			}
		}
	}
	
	
	
	public void execute(Priority priority, Task task){
		if (task instanceof DelayedTask){
			throw new RuntimeException("Task already delayed.");
		}
		if (workers.length == 0){
			task.execute();
		}else {
			createDelayedTask(priority,task);
		}
	}
	public DelayedTask createDelayedTask(Priority priority,Task task) { 
		if (task instanceof DelayedTask){
			return (DelayedTask)task;
		}
		try {
			DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
			ByteArrayOutputStream os = new ByteArrayOutputStream(); 
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(task);
			de.setPriority(priority.getValue());
			de.setData(new ByteArrayInputStream(os.toByteArray()));
			de.setLastError(new StringReader("Debug Message: Class is " + task.getClass().getName() ));
			de.save();
			return de;
		} catch (IOException ex) {
			throw new RuntimeException(task.getClass().getName() ,ex);
		}
	}
}
