package com.venky.swf.plugins.background.core.workers;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class DelayedTaskPollingThread extends Thread{
	private final DelayedTaskManager manager ;
	private final ModelReflector<DelayedTask> ref = ModelReflector.instance(DelayedTask.class);
	public DelayedTaskPollingThread(DelayedTaskManager manager){
		super("DelayedTaskPollingThread");
		setDaemon(false);
		this.manager = manager;
	}
	
	private Expression getWhereClause(DelayedTask lastRecord){
		Expression where = new Expression(ref.getPool(),Conjunction.OR);
		for (int i = 0 ; i < DelayedTask.DEFAULT_ORDER_BY_COLUMNS.length ; i++ ){
			String gtF = DelayedTask.DEFAULT_ORDER_BY_COLUMNS[i];
			Expression part = new Expression(ref.getPool(),Conjunction.AND);
			for (int j = 0 ; j < i  ; j ++){
				String f = DelayedTask.DEFAULT_ORDER_BY_COLUMNS[j];
				part.add(new Expression(ref.getPool(),f, Operator.EQ, lastRecord.getRawRecord().get(f)));
			}
			
			part.add(new Expression(ref.getPool(),gtF, Operator.GT, lastRecord.getRawRecord().get(gtF)));
			where.add(part);
		}
		return where;
	}
	
	public static final int MAX_TASKS_TO_BUFFER = 10000;
	@Override
	public void run(){
		DelayedTask lastRecord = null;
		Database db = null ;
		while (manager.needMoreTasks()){
			List<DelayedTask> jobs = null;
			try {
				Config.instance().getLogger(getClass().getName()).finest("Checking for Tasks...");
				Expression where = new Expression(ref.getPool(),Conjunction.AND);
				where.add(new Expression(ref.getPool(),ref.getColumnDescriptor("NUM_ATTEMPTS").getName(), Operator.LT , 10 ));
				if (lastRecord != null){
					where.add(getWhereClause(lastRecord));
				}
				
				db = Database.getInstance();
				List<String> realColumns = ref.getRealColumns();
				realColumns.remove("DATA");
				realColumns.remove("LAST_ERROR"); //Remove Clob and Blob Columns.
				Select select = new Select(realColumns.toArray(new String[]{})).from(DelayedTask.class).
						where(where).
						orderBy(DelayedTask.DEFAULT_ORDER_BY_COLUMNS);
				jobs = select.execute(DelayedTask.class,MAX_TASKS_TO_BUFFER);
				
				Config.instance().getLogger(getClass().getName()).finest("Number of tasks found:" + jobs.size());

				db.getCurrentTransaction().commit();
				if (jobs.size() < MAX_TASKS_TO_BUFFER){
					lastRecord = null;
				}else {
					lastRecord = jobs.get(jobs.size()-1);
				}
			}catch (Exception e){
				if (db != null){
					Config.instance().getLogger(getClass().getName()).info("Polling thread Rolling back due to exception 1 " + e.toString());
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
				if (jobs != null){
					manager.addDelayedTasks(jobs);
				}
			}
		}

	}
}
