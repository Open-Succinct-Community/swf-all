package com.venky.swf.plugins.background.core;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskHolder implements Task {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5587808806039003066L;

	private Task task = null;
	private Priority taskPriority = null;
	private int taskId = -1; 
	
	@Override
	public Priority getTaskPriority() {
		return taskPriority;
	}
	
	@Override
	public int getTaskId(){ 
		return taskId;
	}
	
	
	private static AtomicInteger fakeIdGenerator = new AtomicInteger();
	public TaskHolder(Task task){
		this.task = task;
		this.taskPriority = (task.getTaskPriority() == null)? Priority.DEFAULT : task.getTaskPriority();
		this.taskId = task.getTaskId() > 0 ? task.getTaskId() : fakeIdGenerator.incrementAndGet();
	}
	

	@Override
	public void execute() {
		task.execute();
	}

}
