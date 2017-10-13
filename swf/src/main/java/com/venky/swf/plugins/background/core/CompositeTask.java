package com.venky.swf.plugins.background.core;

import java.util.LinkedList;

public class CompositeTask implements Task {
	private final LinkedList<Task> tasks ;
	private static final long serialVersionUID = -6287257141086371584L;
	

	public CompositeTask(){
		this.tasks = new LinkedList<>();
	}
	private Priority priority = Priority.LOW;
	public CompositeTask(Task...tasks ) {
		this.tasks = new LinkedList<>(); 
		for (Task t : tasks){
			this.tasks.add(t);
			if (priority.getValue() > t.getTaskPriority().getValue()){ //Lower the value higher the priority.
				priority = t.getTaskPriority();
			}
		}
	}
	@Override
	public Priority getTaskPriority(){
		return priority;
	}
	
	@Override
	public void execute() {
		for (Task task: tasks){
			task.execute();
		}
	}
	
}
