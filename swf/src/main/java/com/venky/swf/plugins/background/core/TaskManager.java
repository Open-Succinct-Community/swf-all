package com.venky.swf.plugins.background.core;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.routing.Config;

import java.util.Collection;
import java.util.List;

public class TaskManager implements _TaskManager{
	static {
    	Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown",new Extension(){
			@Override
			public void invoke(Object... context) {
				Config.instance().getLogger(TaskManager.class.getName()).info("Shutdown Task manager");
				_instance.shutdown();
			}
    	});
    }
	private final static TaskManager _instance = new TaskManager();
	public static TaskManager instance(){
		return _instance;
	}

	@Deprecated
	public <T extends CoreTask> void executeDelayed(T task) {
		executeDelayed(List.of(task));
	}
	public <T extends CoreTask>  void executeAsync(T task){
		executeAsync(List.of(task));
	}

	public <T extends CoreTask> void executeDelayed(Collection<T> tasks){
		executeAsync(tasks, true);
	}
	public <T extends CoreTask> void executeAsync(Collection<T> tasks){
		executeAsync(tasks, false);
	}
	
	public <T extends CoreTask> void executeAsync(T task,boolean persistTaskQueue){
		executeAsync(List.of(task), persistTaskQueue);
	}
	public <T extends CoreTask> void executeAsync(Collection<T> tasks, boolean persistTaskQueue){
		executeAsync(tasks,persistTaskQueue,true);
	}
	public <T extends CoreTask> void executeAsync(Collection<T> tasks, boolean persistTaskQueue, boolean waitForCommit){
		if (persistTaskQueue || waitForCommit) {
			AsyncTaskManagerFactory.getInstance().executeAsync(tasks, persistTaskQueue);
		}else {
			AsyncTaskManagerFactory.getInstance().addAll(tasks);
		}
	}
	
	public void shutdown(){
		AsyncTaskManagerFactory.getInstance().shutdown();
	}
	
	public void execute(Runnable runnable) {
		runnable.run();
	}
	@Override
	public void submit(Runnable runnable) {
		CoreTask coreTask = (runnable instanceof CoreTask)? (CoreTask)runnable : new RunnerTask(runnable);
		AsyncTaskManagerFactory.getInstance().addAll(List.of(coreTask));
	}
	
	public static class RunnerTask implements CoreTask {
		Runnable runnable ;
		public RunnerTask(Runnable runable){
			this.runnable = runable;
		}
		
		@Override
		public void execute() {
			runnable.run();
		}
	}
	
}
