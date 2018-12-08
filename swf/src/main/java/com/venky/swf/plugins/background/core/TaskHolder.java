package com.venky.swf.plugins.background.core;

import java.util.concurrent.atomic.AtomicLong;

public class TaskHolder implements Task {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5587808806039003066L;

	private Task task = null;
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

	public TaskHolder(Task task){
		this.task = task;
		this.taskPriority = (task.getTaskPriority() == null)? Priority.DEFAULT : task.getTaskPriority();
		this.taskId = task.getTaskId() > 0 ? task.getTaskId() : fakeIdGenerator.incrementAndGet();
	}

	public Task innerTask(){
		return task;
	}
	

	@Override
	public void execute() {
		task.execute();
	}

}
