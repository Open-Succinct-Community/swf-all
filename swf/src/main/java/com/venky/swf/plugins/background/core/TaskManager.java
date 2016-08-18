package com.venky.swf.plugins.background.core;

import java.util.Arrays;
import java.util.Collection;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.plugins.background.core.Task.Priority;
import com.venky.swf.plugins.background.db.model.DelayedTask;
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

	public void execute(Task task){
		task.execute();
	}
	@Deprecated
	public void executeDelayed(Task task) {
		executeAsync(task);
	}
	public void executeAsync(Task task){
		executeAsync(Arrays.asList(task)); 
	}
	
	public void executeAsync(Collection<Task> tasks){
		executeAsync(tasks, Priority.DEFAULT); 
	}
	
	public void executeAsync(Task task,Priority priority){
		executeAsync(Arrays.asList(task), priority);
	}
	
	public void executeAsync(Collection<Task> tasks,Priority priority){
		executeAsync(tasks, priority, true);
	}
	
	public void executeAsync(Task task,boolean persistTaskQueue){
		executeAsync(Arrays.asList(task),persistTaskQueue); 
	}
	
	public void executeAsync(Collection<Task> tasks,boolean persistTaskQueue){
		executeAsync(tasks, Priority.DEFAULT,persistTaskQueue); 
	}
	
	public void executeAsync(Task task,Priority priority, boolean persistTaskQueue){
		executeAsync(Arrays.asList(task), priority,persistTaskQueue);
	}
	
	public void executeAsync(Collection<Task> tasks,Priority priority,boolean persistTaskQueue){
		if (persistTaskQueue){
			AsyncTaskManager.getInstance(DelayedTask.class).execute(tasks, priority);
		}else {
			AsyncTaskManager.getInstance(TaskHolder.class).execute(tasks, priority);
		}
	}
	
	public void shutdown(){
		AsyncTaskManager.shutdownAll();
	}
	public void wakeUp(){
		AsyncTaskManager.wakeUpAll();
	}
}
