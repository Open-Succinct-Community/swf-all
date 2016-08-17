package com.venky.swf.plugins.background.core.workers;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.core.log.TimerStatistics;
import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;

public class DelayedTaskWorker extends Thread {
	private final DelayedTaskManager manager ; 
	public DelayedTaskWorker(DelayedTaskManager manager,int instanceNumber){
		super("DelayedTaskWorker:" + instanceNumber);
		setDaemon(false);
		this.manager = manager;
	}
	private Logger cat = Config.instance().getLogger(getClass().getName());
	private void log(Level level,String message){
		if (cat.isLoggable(level)){
			cat.log(level, "Thread :" + getName() + ":" + message);
		}
	}
	public void run(){
		DelayedTask task = null ;
		while ((task = manager.next()) != null ){
			TimerStatistics.setEnabled(Config.instance().isTimerEnabled());
			log(Level.INFO,"Started Task:" + task.getId());
			Database db = null; 
			Transaction txn = null;
			try {
				db = Database.getInstance();
				db.open(task.getCreatorUser());
				txn = db.getCurrentTransaction();
				task.execute();
				txn.commit();
			}catch (Throwable e){
				log(Level.INFO,"Worker thread Rolling back due to exception " + ExceptionUtil.getRootCause(e).toString());
				try {
					if (txn != null) {
						txn.rollback(e);
					}
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}finally{
				try {
					log(Level.INFO,"Completed Task:" + task.getId());
					if (db != null) {
						db.close();
					}
					TimerStatistics.dumpStatistics();
				}catch(Exception ex) {
					MultiException m = new MultiException("Error while closing connections");
					m.add(ex);
					m.printStackTrace();
				}
			}
		}
	}
	
	
}
