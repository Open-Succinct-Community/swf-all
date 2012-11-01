package com.venky.swf.plugins.background.core.workers;

import java.util.List;
import java.util.logging.Logger;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class DelayedTaskPollingThread extends Thread{
	private final DelayedTaskManager manager ;
	public DelayedTaskPollingThread(DelayedTaskManager manager){
		super("DelayedTaskPollingThread");
		setDaemon(false);
		this.manager = manager;
	}
	
	private Expression getWhereClause(DelayedTask lastRecord){
		Expression where = new Expression(Conjunction.OR);
		for (int i = 0 ; i < DelayedTask.DEFAULT_ORDER_BY_COLUMNS.length ; i++ ){
			String gtF = DelayedTask.DEFAULT_ORDER_BY_COLUMNS[i];
			Expression part = new Expression(Conjunction.AND);
			for (int j = 0 ; j < i  ; j ++){
				String f = DelayedTask.DEFAULT_ORDER_BY_COLUMNS[j];
				part.add(new Expression(f, Operator.GE, lastRecord.getRawRecord().get(f)));
			}
			
			part.add(new Expression(gtF, Operator.GT, lastRecord.getRawRecord().get(gtF)));
			where.add(part);
		}
		return where;
	}
	
	@Override
	public void run(){
		ModelReflector<DelayedTask> ref = ModelReflector.instance(DelayedTask.class);
		DelayedTask lastRecord = null;
		Database db = null ;
		while (manager.needMoreTasks()){
			try {
				Logger.getLogger(getClass().getName()).finest("Checking for Tasks...");
				Expression where = new Expression(Conjunction.AND);
				where.add(new Expression(ref.getColumnDescriptor("NUM_ATTEMPTS").getName(), Operator.LT , 10 ));
				if (lastRecord != null){
					where.add(getWhereClause(lastRecord));
				}
				
				db = Database.getInstance();
				Select select = new Select().from(DelayedTask.class).
						where(where).
						orderBy(DelayedTask.DEFAULT_ORDER_BY_COLUMNS);
				List<DelayedTask> jobs = select.execute(DelayedTask.class,100);
				
				Logger.getLogger(getClass().getName()).finest("Number of tasks found:" + jobs.size());

				manager.addDelayedTasks(jobs);
				db.getCurrentTransaction().commit();
				if (jobs.size() < 100){
					lastRecord = null;
				}else {
					lastRecord = jobs.get(jobs.size()-1);
				}
			}catch (Exception e){
				if (db != null){
					Logger.getLogger(getClass().getName()).info("Polling thread Rolling back due to exception 1 " + e.toString());
					try {
						db.getCurrentTransaction().rollback(e);
					}catch (Exception ex){
						ex.printStackTrace();
					}
				}
			}finally{
				if (db != null){
					db.close();
					db = null;
				}
			}
		}

	}
}
