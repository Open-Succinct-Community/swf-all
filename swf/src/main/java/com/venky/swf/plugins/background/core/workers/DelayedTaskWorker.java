package com.venky.swf.plugins.background.core.workers;

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
			Database db = Database.getInstance();
			try {
				task.execute();
				db.getCurrentTransaction().commit();
			}finally{
				db.close();
			}
		}
	}
}
