package com.venky.swf.plugins.background.controller;

import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.string.StringUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.integration.JSON;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.SerializationHelper;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.json.JSONObjectWrapper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

}
