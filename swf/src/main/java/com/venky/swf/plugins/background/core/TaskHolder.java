package com.venky.swf.plugins.background.core;

import com.venky.swf.routing.Config;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;

public class TaskHolder implements CoreTask {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5587808806039003066L;


	@Override
	public void execute() {
		Config.instance().setHostName(hostName);
		task.execute();
	}
	
	@Override
	public String getTaskIdentifier() {
		return task.getTaskIdentifier();
	}
	
	@Override
	public void onStart() {
		log("Task Execution Started");
		task.onStart();
	}

	@Override
	public void onSuccess() {
		log("Task Execution Successful");
		task.onSuccess();
	}

	@Override
	public void onException(Throwable ex) {
		log("Task Executed with exception ", ex);
		task.onException(ex);
	}

	@Override
	public void onComplete() {
		log("Task Finished ");
		task.onComplete();
	}


	@Override
	public int compareTo(CoreTask o) {
		return task.compareTo(o);
	}

	@Override
	public AsyncTaskManager getAsyncTaskManager() {
		return task.getAsyncTaskManager();
	}



	private CoreTask task = null;
	private Priority taskPriority = null;
	private long taskId = -1;
	
	@Override
	public Priority getTaskPriority() {
		return taskPriority;
	}
	
	@Override
	public long getTaskId(){
		return taskId;
	}
	
	
	private static final AtomicLong fakeIdGenerator = new AtomicLong();
	public TaskHolder(CoreTask task){
		this.task = task;
		this.taskPriority = (task.getTaskPriority() == null)? Priority.DEFAULT : task.getTaskPriority();
		this.taskId = task.getTaskId() > 0 ? task.getTaskId() : fakeIdGenerator.incrementAndGet();
		this.hostName = Config.instance().getHostName();
	}
	
	String hostName = null;
	public String getHostName() {
		return hostName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TaskHolder that = (TaskHolder) o;

		if (taskId != that.taskId) return false;
		if (!task.equals(that.task)) return false;
        return taskPriority == that.taskPriority;
    }

	@Override
	public int hashCode() {
		int result = task.hashCode();
		result = 31 * result + taskPriority.hashCode();
		result = 31 * result + (int) (taskId ^ (taskId >>> 32));
		return result;
	}
}
