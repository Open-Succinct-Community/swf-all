package com.venky.swf.plugins.background.core;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;
import com.venky.swf.routing.Config;

public class TaskManager{
	static {
    	Registry.instance().registerExtension("com.venky.swf.db.Database.beforeClose",new Extension(){
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
