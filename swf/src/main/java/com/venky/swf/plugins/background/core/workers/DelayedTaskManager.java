package com.venky.swf.plugins.background.core.workers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.PriorityQueue;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.db.model.DelayedTask;
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
	
	private DelayedTaskManager(){
		this(Config.instance().getIntProperty("swf.plugins.background.core.workers.numThreads", 1));
	}
	
	private DelayedTaskManager(int numWorkerThreads){
		super();
		workers = new DelayedTaskWorker[numWorkerThreads];
		for (int i = 0 ; i < workers.length; i ++){
			workers[i] = new DelayedTaskWorker(this);
			workers[i].start();
		}
		dtpt = new DelayedTaskPollingThread(this);
		dtpt.start();
	}
	
	public void wakeUp(){
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
		synchronized (queue) {
			waitIfQueueIsEmpty();
			DelayedTask dt = null ;
			if (!queue.isEmpty()){
				dt = queue.poll();
				queue.notifyAll();
			}
			return dt;
		}
	}
	
	public boolean needMoreTasks(){
		synchronized (queue) {
			if (queue.isEmpty()){
				try {
					Config.instance().getLogger(getClass().getName()).finest("Daemon going to sleep to 1 minute.");
					queue.wait(60*1000); //1 minute;
				}catch (InterruptedException ex){
					//
					Config.instance().getLogger(getClass().getName()).finest("Daemon Woke up");
				}
			}
			waitIfQueueIsNotEmpty();
			return keepAlive();
			
		}
	}

	public void waitIfQueueIsEmpty(){
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
			Config.instance().getLogger(getClass().getName()).finest("Daemon waking up since all pending tasks are complete.");
		}
	}
	
	private boolean shutdown =  false; 
	public void shutdown(){
		synchronized (queue) {
			shutdown = true;
			queue.notifyAll();
		}
	}
	
	
	
	public void execute(Task task){
		if (task instanceof DelayedTask){
			throw new RuntimeException("Task already delayed.");
		}
		try {
			if (workers.length == 0){
				//No Workers. So Syncronous.
				task.execute();
			}else {
				DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
				ByteArrayOutputStream os = new ByteArrayOutputStream(); 
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(task);
				
				de.setData(new ByteArrayInputStream(os.toByteArray()));
				de.save();
			}
		}catch(IOException ex){
			throw new RuntimeException(ex);
		}
	}
}
