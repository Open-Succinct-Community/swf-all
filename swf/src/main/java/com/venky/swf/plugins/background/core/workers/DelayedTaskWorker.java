package com.venky.swf.plugins.background.core.workers;

import java.util.logging.Logger;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.db.model.DelayedTask;

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
			Logger.getLogger(getClass().getName()).info("Started Task:" + task.getId());
			Database db = Database.getInstance();
			try {
				task.execute();
				db.getCurrentTransaction().commit();
			}finally{
				db.getCurrentTransaction().rollback();
				db.close();
				Logger.getLogger(getClass().getName()).info("Completed Task:" + task.getId());
			}
		}
	}
}
