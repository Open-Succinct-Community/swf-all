package com.venky.swf.plugins.background.core.workers;

import java.util.logging.Logger;

import com.venky.core.log.TimerStatistics;
import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;

public class DelayedTaskWorker extends Thread {
	private final DelayedTaskManager manager ; 
	public DelayedTaskWorker(DelayedTaskManager manager){
		super("DelayedTaskWorker");
		setDaemon(false);
		this.manager = manager;
	}
	
	public void run(){
		DelayedTask task = null ;
		while ((task = manager.next()) != null ){
			TimerStatistics.setEnabled(Config.instance().isTimerEnabled());
			Logger.getLogger(getClass().getName()).info("Started Task:" + task.getId());
			Database db = Database.getInstance();
			db.open(task.getCreatorUser());
			Transaction txn = db.getCurrentTransaction();
			try {
				task.execute();
				txn.commit();
			}catch (Throwable e){
				Logger.getLogger(getClass().getName()).info("Worker thread Rolling back due to exception " + ExceptionUtil.getRootCause(e).toString());
				try {
					txn.rollback(e);
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}finally{
				Logger.getLogger(getClass().getName()).info("Completed Task:" + task.getId());
				db.close();
				TimerStatistics.dumpStatistics();
			}
		}
	}
	
	
}
