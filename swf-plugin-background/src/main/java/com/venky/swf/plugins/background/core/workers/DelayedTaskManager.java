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
import com.venky.swf.routing.Router;

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
		this(Config.instance().getIntProperty("swf.plugins.background.core.workers.numThreads", 2));
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
		if (Router.instance().getLoader() != getClass().getClassLoader()){
			return false;
		}
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
					queue.wait(60*1000); //1 minute;
				}catch (InterruptedException ex){
					//
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
					queue.wait();
				}catch (InterruptedException ex){
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
					queue.wait();
				}catch (InterruptedException ex){
					//
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
	}
	
	
	
	public void execute(Task task){
		if (task instanceof DelayedTask){
			throw new RuntimeException("Task already delayed.");
		}
		try {
			DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
			ByteArrayOutputStream os = new ByteArrayOutputStream(); 
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(task);
			
			de.setData(new ByteArrayInputStream(os.toByteArray()));
			de.save();
		}catch(IOException ex){
			throw new RuntimeException(ex);
		}
	}
}
