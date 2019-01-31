package com.venky.swf.plugins.background.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.MultiException;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.routing.Config;

public class AsyncTaskWorker extends Thread{
	private AsyncTaskManager manager ;
	public AsyncTaskWorker(AsyncTaskManager asyncTaskManager, int instanceNumber) {
		super(asyncTaskManager.getClass().getSimpleName() + ":" + instanceNumber);
		setDaemon(false);
		this.manager = asyncTaskManager;
		setPriority(Thread.MAX_PRIORITY);
	}
	private SWFLogger cat = Config.instance().getLogger(getClass().getName());
	private void log(Level level,String message){
		if (cat.isLoggable(level)){
			cat.log(level, "Thread :" + getName() + ":" + message);
		}
	}
	protected <T extends Task> String getTaskIdentifier(T task){
		if (task instanceof  TaskHolder) {
			return ((TaskHolder)task).innerTask().getClass().getName();
		}else {
			return task.getClass().getName();
		}
	}
	public void run(){
		Task task = null ;
		while ((task = manager.next()) != null ){
			log(Level.INFO,"Started Task:" + getTaskIdentifier(task));
			Database db = null; 
			Transaction txn = null;
			Timer timer = cat.startTimer(getTaskIdentifier(task));
			try {
				db = Database.getInstance();
				txn = db.getCurrentTransaction();
				task.execute();
				txn.commit();
			}catch (Throwable e){
				StringWriter sw = new StringWriter();
				PrintWriter p = new PrintWriter(sw);
				ExceptionUtil.getRootCause(e).printStackTrace(p); p.close();
				log(Level.WARNING,"Worker thread Rolling back due to exception " + sw.toString());
				try {
					if (txn != null) {
						txn.rollback(e);
					}
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}finally{
				timer.stop();
				try {
					log(Level.INFO,"Completed Task:" + getTaskIdentifier(task));
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