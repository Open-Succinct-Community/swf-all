package com.venky.swf.plugins.background.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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

	public <T extends CoreTask> void execute(T task){
		task.execute();
	}
	@Deprecated
	public <T extends CoreTask> void executeDelayed(T task) {
		executeAsync(task);
	}
	public <T extends CoreTask>  void executeAsync(T task){
		executeAsync(task,true); 
	}

	public <T extends CoreTask> void executeAsync(T task,boolean persistTaskQueue){
		executeAsync(Collections.singleton(task), persistTaskQueue);
	}
	
	public <T extends CoreTask> void executeAsync(Collection<T> tasks){
		executeAsync(tasks, true); 
	}

	public <T extends CoreTask> void executeAsync(Collection<T> tasks, boolean persistTaskQueue){
		AsyncTaskManagerFactory.getInstance().get(AsyncTaskManager.class).execute(tasks, persistTaskQueue);
	}
	
	public void shutdown(){
		AsyncTaskManagerFactory.getInstance().shutdown();
	}
	public void wakeUp(){
		AsyncTaskManagerFactory.getInstance().wakeUp();
	}
}
