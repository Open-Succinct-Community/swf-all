package com.venky.swf.plugins.background.controller;

import java.io.StringReader;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.views.View;

public class DelayedTasksController extends ModelController<DelayedTask> {

	public DelayedTasksController(Path path) {
		super(path);
	}
	
	@SingleRecordAction(tooltip="Retry")
	public View retry(long id){
		DelayedTask task = Database.getTable(DelayedTask.class).get(id);
		task.setNumAttempts(0);
		task.setLastError(new StringReader(""));
		task.save();
		return back();
	}

	@RequireLogin(false)
	public View trigger(){
		TaskManager.instance().wakeUp();
		if (getIntegrationAdaptor() != null){
    		return getIntegrationAdaptor().createStatusResponse(getPath(), null);
		}else {
			return back();
		}
	}
}
