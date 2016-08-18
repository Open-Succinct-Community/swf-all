package com.venky.swf.plugins.background.core.workers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.Collection;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.Task.Priority;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;

public class DelayedTaskManager extends AsyncTaskManager<DelayedTask>{
	private final DelayedTaskPollingThread dtpt ;
	
	public DelayedTaskManager(){
		super();
		incrementPollerCount();
		dtpt = new DelayedTaskPollingThread(this);
		dtpt.start();
	}
	
	private Bucket numPollingThreadsWorking = new Bucket();
	
	public void wakeUp(){
		waitUntillPollersFinish();
		super.wakeUp();
	}
	
	public boolean needMoreTasks(boolean tasksFoundInLastRun){
		decrementPollerCount();
		if (!tasksFoundInLastRun){
			waitIfQueueIsEmpty(2*60);
		}
		waitUntilQueueIsEmpty();
		waitUntilWorkersFinish(); // Avoid Locking with Workers.
		boolean keepAlive = keepAlive();
		if (keepAlive) {
			incrementPollerCount();
		}
		return keepAlive;
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
	
	public void waitUntillPollersFinish(){
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
	
	public void shutdown(){
		super.shutdown();
		while (true) {
			try {
				dtpt.join();
				break;
			} catch (InterruptedException e) {
			}
		}
		Config.instance().getLogger(getClass().getName()).info("Polling Thread has shutdown");
	}
	protected void pushAsyncTasks(Collection<Task> tasks, Priority priority) {
		for (Task task : tasks) {
			try {
				DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
				ByteArrayOutputStream os = new ByteArrayOutputStream(); 
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(task);
				de.setPriority(Priority.DEFAULT.getValue());
				de.setData(new ByteArrayInputStream(os.toByteArray()));
				de.setLastError(new StringReader("Debug Message: Class is " + task.getClass().getName() ));
				de.save();
			} catch (IOException ex) {
				throw new RuntimeException(task.getClass().getName() ,ex);
			}
		}
	}
}
