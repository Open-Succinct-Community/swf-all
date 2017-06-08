package com.venky.swf.plugins.background.core;

import java.util.Arrays;
import java.util.Collection;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.routing.Config;

public class TaskManager{
	static {
    	Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown",new Extension(){
			@Override
			public void invoke(Object... context) {
				Config.instance().getLogger(TaskManager.class.getName()).info("Shutdown Task manager");
				_instance.shutdown();
			}
    	});
    }
	private static TaskManager _instance = new TaskManager();
	public static TaskManager instance(){
		return _instance;
	}

	public <T extends Task> void execute(T task){
		task.execute();
	}
	@Deprecated
	public <T extends Task> void executeDelayed(T task) {
		executeAsync(task);
	}
	public <T extends Task>  void executeAsync(T task){
		executeAsync(task,true); 
	}

	public <T extends Task> void executeAsync(T task,boolean persistTaskQueue){
		executeAsync(Arrays.asList(task), persistTaskQueue);
	}
	
	public <T extends Task> void executeAsync(Collection<T> tasks){
		executeAsync(tasks, true); 
	}

	public <T extends Task> void executeAsync(Collection<T> tasks, boolean persistTaskQueue){
		AsyncTaskManager.getInstance().execute(tasks, persistTaskQueue);
	}
	
	public void shutdown(){
		AsyncTaskManager.getInstance().shutdown();
	}
	public void wakeUp(){
		AsyncTaskManager.getInstance().wakeUp();
	}
}
