package com.venky.swf.plugins.background.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.venky.core.io.SeekableByteArrayOutputStream;
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
import com.venky.swf.plugins.background.core.DbTaskManager;
import com.venky.swf.plugins.background.core.SerializationHelper;
import com.venky.swf.plugins.background.core.IOTaskManager;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import javax.servlet.http.HttpServletRequest;

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
	private JSON getCriteria() {
        try {
            InputStream is = getPath().getInputStream();
            JSON criteria = new JSON(is);
            return criteria;
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }

    }

    @RequireLogin
	public View next(){
		HttpServletRequest request = getPath().getRequest();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new RuntimeException("Cannot call save in any other method other than POST");
        }
        JSON criteria = getCriteria();


		// boolean wait = Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().valueOf(criteria.getAttribute("Wait");
		//wait  should not be from request.

        int batch = Database.getJdbcTypeHelper("").getTypeRef(Integer.class).getTypeConverter().valueOf(criteria.getAttribute("BatchSize"));

        batch = Math.max(batch,1);
        CoreTask task = null;
        List<CoreTask> tasks = new ArrayList<>();
		while (((task = AsyncTaskManagerFactory.getInstance().get(AsyncTaskManager.class).next(false, false)) != null) && tasks.size() <= batch ){
		    tasks.add(task);
        }

		SeekableByteArrayOutputStream byteArrayOutputStream = new SeekableByteArrayOutputStream();
		SerializationHelper helper = new SerializationHelper();
		helper.write(byteArrayOutputStream,tasks);
		return new BytesView(getPath(),byteArrayOutputStream.toByteArray(), MimeType.APPLICATION_OCTET_STREAM);
	}

	@RequireLogin
	public View push(){
		HttpServletRequest request = getPath().getRequest();
		if (!request.getMethod().equalsIgnoreCase("POST")) {
			throw new RuntimeException("Cannot call save in any other method other than POST");
		}
		try {
			InputStream is = getPath().getInputStream();
			List<Task> tasks = new SerializationHelper().read(is);
			TaskManager.instance().executeAsync(tasks,false);
		}catch (IOException ex){
			return getIntegrationAdaptor().createStatusResponse(getPath(),ex);
		}
		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}

	@RequireLogin
	public View addWorker(long incrementBy){
		for (int i = 0 ; i< incrementBy ; i ++){
			AsyncTaskManagerFactory.getInstance().get(IOTaskManager.class).addWorker();
			AsyncTaskManagerFactory.getInstance().get(DbTaskManager.class).addWorker();
		}
		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}

	@RequireLogin
	public View evict(int numWorkers){
		AsyncTaskManagerFactory.getInstance().get(IOTaskManager.class).evictWorker(numWorkers);
		AsyncTaskManagerFactory.getInstance().get(DbTaskManager.class).evictWorker(numWorkers);
		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}
}
