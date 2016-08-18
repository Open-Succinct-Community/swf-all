package com.venky.swf.plugins.background.core.workers;

import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskWorker;
import com.venky.swf.plugins.background.db.model.DelayedTask;

public class DelayedTaskWorker extends AsyncTaskWorker<DelayedTask> {
	public DelayedTaskWorker(AsyncTaskManager<DelayedTask> manager,int instanceNumber){
		super(manager,instanceNumber);
	}
	protected String getTaskIdentifier(DelayedTask task){
		return String.valueOf(task.getId());
	}
}
