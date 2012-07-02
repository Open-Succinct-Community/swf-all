package com.venky.swf.plugins.background.core;

import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;

public class TaskManager{
	private static TaskManager _instance = new TaskManager();
	public static TaskManager instance(){
		return _instance;
	}

	public void execute(Task task){
		task.execute();
	}
	public void executeDelayed(Task task){
		DelayedTaskManager.instance().execute(task);
	}
	
	public void shutdown(){
		DelayedTaskManager.instance().shutdown();
	}
	public void wakeUp(){
		DelayedTaskManager.instance().wakeUp();
	}
}
