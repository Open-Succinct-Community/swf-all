package com.venky.swf.plugins.background.core;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.threadpool.TrackedExecutorServiceManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.plugins.background.extensions.InMemoryTaskQueueManager;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class AsyncTaskManager  {
	public AsyncTaskManager() {
	
	}
	
	
	public List<Future<?>> addAll(Collection<? extends CoreTask> tasks) {
		if (tasks.isEmpty()) {
			return List.of();
		}
		if (service.isShutdown()) {
			throw new RuntimeException("Execution Service Shutting down!");
		}
		return tasks.stream().map((task)->service.submit(task)).collect(Collectors.toList());
	}

	public void shutdown() {
		service.shutdown();
	}
	public boolean isShutdown(){
		return service.isShutdown() && service.isTerminated();
	}

	public <T extends CoreTask>  void execute(Collection<T> tasks){
		execute(tasks, false);
	}
	
	public <T extends CoreTask> void execute(Collection<T> tasks, boolean persist){
		pushAsyncTasks(tasks, persist);
	}
	protected <T extends CoreTask> void pushAsyncTasks(Collection<T> tasks, boolean persist) {
		if (tasks.isEmpty()) {
			return;
		}
		if (!persist) {
            List<CoreTask> taskHolders = new LinkedList<CoreTask>();
            for (CoreTask task : tasks){
                taskHolders.add(new TaskHolder(task));
            }
            //addAll(taskHolders);
            InMemoryTaskQueueManager.getPendingTasks().addAll(taskHolders); //To ensures tasks are executed after commit.
		}else {
			SerializationHelper helper = new SerializationHelper();
			for (CoreTask task: tasks){
				DelayedTask de = Database.getTable(DelayedTask.class).newRecord();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				helper.write(os,task);
				//ObjectOutputStream oos = new ObjectOutputStream(os);
				de.setPriority(task.getTaskPriority().getValue());
				de.setTaskClassName(task.getClass().getName());
				de.setData(new ByteArrayInputStream(os.toByteArray()));
				de.save();
			}
			//Database.getInstance().getCurrentTransaction().setAttribute(PersistedTaskPollingAgent.class.getName()+".trigger", true);
			//Trigger PersistedTaskPollingAgent through Cron. KEep loosely coupled.
		}
	}
	
	TrackedExecutorServiceManager service = new TrackedExecutorServiceManager();
	
	
	public int count(){
		return service.count();
	}
	
	
}
