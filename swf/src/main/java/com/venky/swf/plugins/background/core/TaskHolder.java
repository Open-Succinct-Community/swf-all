package com.venky.swf.plugins.background.core;

import java.util.concurrent.atomic.AtomicLong;

public class TaskHolder implements CoreTask {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5587808806039003066L;

	@Override
	public void onStart() {
		task.onStart();
	}

	@Override
	public void execute() {
		task.execute();
	}

	@Override
	public void onSuccess() {
		task.onSuccess();
	}

	@Override
	public void onException(Throwable ex) {
		task.onException(ex);
	}

	@Override
	public void onComplete() {
		task.onComplete();
	}

	@Override
	public boolean canExecuteRemotely() {
		return task.canExecuteRemotely();
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
	
	
	private static AtomicLong fakeIdGenerator = new AtomicLong();
	@Deprecated //Kept for Kryo Serialization
	public TaskHolder(){

	}

	public TaskHolder(CoreTask task){
		this.task = task;
		this.taskPriority = (task.getTaskPriority() == null)? Priority.DEFAULT : task.getTaskPriority();
		this.taskId = task.getTaskId() > 0 ? task.getTaskId() : fakeIdGenerator.incrementAndGet();
	}

	public CoreTask innerTask(){
		return task;
	}






}
