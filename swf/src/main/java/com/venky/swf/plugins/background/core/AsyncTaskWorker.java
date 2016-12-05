package com.venky.swf.plugins.background.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.core.log.TimerStatistics;
import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.routing.Config;

public class AsyncTaskWorker<T extends Task & Comparable<? super T>> extends Thread{
	private AsyncTaskManager<T> manager ;
	public AsyncTaskWorker(AsyncTaskManager<T> asyncTaskManager, int instanceNumber) {
		super(asyncTaskManager.getClass().getSimpleName() + ":" + instanceNumber);
		setDaemon(false);
		this.manager = asyncTaskManager;
	}
	private Logger cat = Config.instance().getLogger(getClass().getName());
	private void log(Level level,String message){
		if (cat.isLoggable(level)){
			cat.log(level, "Thread :" + getName() + ":" + message);
		}
	}
	protected String getTaskIdentifier(T task){
		return task.getClass().getName();
	}
	public void run(){
		T task = null ;
		while ((task = manager.next()) != null ){
			TimerStatistics.setEnabled(Config.instance().isTimerEnabled());
			log(Level.INFO,"Started Task:" + getTaskIdentifier(task));
			Database db = null; 
			Transaction txn = null;
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