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
	private boolean persistTaskQueue = false;
	public CompositeBatchTask(List<Task> tasks){
		this(tasks,0);
	}
	public CompositeBatchTask(List<Task> tasks, int batchSize) {
        this(tasks,batchSize,false);
    }
    public CompositeBatchTask(List<Task> tasks, int batchSize, boolean persistTaskQueue) {
		this.batchSize = batchSize;
		this.tasks.addAll(tasks);
		this.persistTaskQueue = persistTaskQueue;
	}
	
	public CompositeBatchTask() {
		
		
	}
    @Override
	public void execute() {
        if (this.batchSize <=0 ){
            this.batchSize = 2;
        }
        List<Task> batch = new ArrayList<>();
		Iterator<Task> ti = tasks.iterator();
		for (int i = 0 ; i < batchSize && ti.hasNext() ; i ++ ) {
			Task next = ti.next();
			batch.add(next);
			ti.remove();
		}
		if (ti.hasNext()) {
			batch.add(new CompositeBatchTask(tasks,batchSize,persistTaskQueue));
		}
		TaskManager.instance().executeAsync(new CompositeTask(batch.toArray(new Task[]{})), persistTaskQueue);
	}
}
