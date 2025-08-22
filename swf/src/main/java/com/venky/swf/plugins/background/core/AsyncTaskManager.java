package com.venky.swf.plugins.background.core;

import com.venky.cache.Cache;
import com.venky.cache.UnboundedCache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.model.io.json.JSONModelReader;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.threadpool.WeightedPriorityQueueVirtualThreadExecutor;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.plugins.background.extensions.InMemoryTaskQueueManager;
import com.venky.swf.routing.Config;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class AsyncTaskManager  {
	WeightedPriorityQueueVirtualThreadExecutor service = new WeightedPriorityQueueVirtualThreadExecutor();
	
	public AsyncTaskManager() {
	
	}
	
	

	public void addAll(Collection<? extends CoreTask> tasks) {
		if (tasks.isEmpty()) {
			return;
		}
		if (service.isShutdown()) {
			throw new RuntimeException("Execution Service Shutting down!");
		}
		for (CoreTask task : tasks){
			service.submit(task);
		}
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
	public int count(){
		return service.getQueue().size();
	}
	
	
}
