package com.venky.swf.plugins.background.db.model;

import com.venky.core.io.StringReader;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.DbTaskManager;
import com.venky.swf.plugins.background.core.SerializationHelper;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.routing.Config;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DelayedTaskImpl extends ModelImpl<DelayedTask> {
	public DelayedTaskImpl(){
		//For Serialization.
	}

	public DelayedTaskImpl(DelayedTask proxy) {
		super(proxy);
	}
	
	public Priority getTaskPriority(){
		return CoreTask.getPriority(getProxy().getPriority());
	}
	public long getTaskId(){
		return getProxy().getId();
	}
	
	public Task getContainedTask() {
		DelayedTask proxy = getProxy();
		SerializationHelper helper = new SerializationHelper();
		try {
			InputStream is = proxy.getData();
			Task task = null;
			if (is != null) {
				task = helper.read(is);
				is.close();
			}
			return task;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    

    public void execute(){
		DelayedTask o = getProxy();
		Transaction parentTxn = Database.getInstance().getTransactionManager().createTransaction();
		try { 
			DelayedTask locked = Database.getTable(DelayedTask.class).lock(o.getId(),false);
			SerializationHelper helper = new SerializationHelper();
			if (locked != null) {
				boolean success = false;
				Transaction txn  = null;
				try {
					InputStream is = locked.getData();
					Task task = helper.read(is);
					is.close();
					Config.instance().getLogger(getClass().getName()).info("Executing " + task.getClass().getName() + " : DelayedTask#" + o.getId() );
					txn = Database.getInstance().getTransactionManager().createTransaction();
					task.execute();
					txn.commit();
					success = true;
				}catch(Exception ex){
					StringWriter sw = new StringWriter();
					PrintWriter w = new PrintWriter(sw);
					ex.printStackTrace(w);
					Config.instance().getLogger(getClass().getName()).warning(sw.toString());
					txn.rollback(ex);
					locked.setLastError(new StringReader(sw.toString()));
					locked.setNumAttempts(locked.getNumAttempts()+1);
				}
				if (success){
					locked.destroy();
				}else {
					locked.save();
				}
			}
			parentTxn.commit();
		}catch (Exception ex){
			parentTxn.rollback(ex);
		}
	}
	public AsyncTaskManager getAsyncTaskManager(){
		return getContainedTask().getAsyncTaskManager();
	}

	public void onStart() {
		Database.getInstance().getCurrentTransaction();
	}

	public void onException(Throwable ex) {
		Database.getInstance().getCurrentTransaction().rollback(ex);
	}

	public void onSuccess() {
		Database.getInstance().getCurrentTransaction().commit();
	}

	public void onComplete() {
		Database.getInstance().close();
	}

	public boolean canExecuteRemotely() {
		return true;
	}
}