package com.venky.swf.plugins.background.core;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskHolder implements Task, Comparable<TaskHolder>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5587808806039003066L;

	private Task task = null;
	private Priority priority = null;
	public Priority getPriority() {
		return priority;
	}


	private int id = -1; 

	private static AtomicInteger fakeIdGenerator = new AtomicInteger();
	public TaskHolder(Task task, Priority priority){
		this.task = task;
		this.priority = priority;
		this.id = fakeIdGenerator.incrementAndGet();
	}
	

	@Override
	public void execute() {
		task.execute();
	}


	@Override
	public int compareTo(TaskHolder o) {
		int ret  = getPriority().compareTo(o.getPriority()); 
		if (ret == 0) {
			ret = Integer.compare(id, o.id);
		}
		return ret;
	}

	
	

}
