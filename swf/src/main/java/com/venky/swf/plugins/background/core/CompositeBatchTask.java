package com.venky.swf.plugins.background.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CompositeBatchTask implements Task{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8912479033438357513L;
	private LinkedList<Task> tasks = new LinkedList<>();
	private int batchSize = 2; 
	public CompositeBatchTask(List<Task> tasks){
		this(tasks,0);
	}
	public CompositeBatchTask(List<Task> tasks, int batchSize) {
		this.batchSize = batchSize;
		this.tasks.addAll(tasks);
		if (this.batchSize <=0 ){ 
			this.batchSize = 2;
		}
	}
	
	public CompositeBatchTask() {
		
		
	}

	@Override
	public void execute() {
		List<Task> batch = new ArrayList<>();
		Iterator<Task> ti = tasks.iterator();
		for (int i = 0 ; i < batchSize && ti.hasNext() ; i ++ ) {
			Task next = ti.next();
			batch.add(next);
			ti.remove();
		}
		if (ti.hasNext()) {
			batch.add(new CompositeBatchTask(tasks));
		}
		TaskManager.instance().executeAsync(new CompositeTask(batch.toArray(new Task[]{})), false);
	}
}
