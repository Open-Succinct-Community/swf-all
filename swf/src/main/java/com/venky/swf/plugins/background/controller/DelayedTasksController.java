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
		InputStream is = getPath().getInputStream();
		return new JSON(is);
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
        CoreTask task ;
        List<CoreTask> tasks = new ArrayList<>();
		while (((task = TaskManager.instance().next(false, false)) != null) && tasks.size() <= batch ){
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
		}catch (Exception ex){
			return getIntegrationAdaptor().createStatusResponse(getPath(),ex);
		}
		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}

	public static class Command extends JSONObjectWrapper {
		public Command(){
			super();
		}

		public int getNumWorkers(){
		    return getInteger("num_workers");
		}
		public void setNumWorkers(int num_workers){
		    set("num_workers",num_workers);
		}

		public String getCategory(){
		    return get("category");
		}
		public void setCategory(String category){
		    set("category",category);
		}



	}
	@SuppressWarnings("unchecked")
	@RequireLogin
	public View addWorker(){
		try {
			Command command = new Command();
			String payload  = StringUtil.read(getPath().getInputStream());
			command.setPayload(payload);
			String clazz = String.format("%sTaskManager",
					command.getCategory());


			AsyncTaskManagerFactory.getInstance().get(clazz).addWorker(command.getNumWorkers());
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}


		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}

	@RequireLogin
	@SuppressWarnings("ALL")
	public View evict(){
		Command command = new Command();
		String payload = "{}" ;
		try {
			payload = StringUtil.read(getPath().getInputStream());
			command.setPayload(payload);
			String clazz = String.format("%s.%sTaskManager",AsyncTaskManager.class.getPackageName(),
					command.getCategory());


			AsyncTaskManagerFactory.getInstance().get((Class <? extends AsyncTaskManager>)Class.forName(clazz)).evictWorker(command.getNumWorkers());
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
		return getIntegrationAdaptor().createStatusResponse(getPath(),null);
	}
}
