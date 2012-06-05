package com.venky.swf.plugins.background.core.workers;

import java.util.List;

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
	
	@Override
	public void run(){
		ModelReflector<DelayedTask> ref = ModelReflector.instance(DelayedTask.class);
		Integer lastId = null;
		while (manager.needMoreTasks()){
			Database db = null ;
			try {
				Expression where = new Expression(Conjunction.AND);
				where.add(new Expression(ref.getColumnDescriptor("NUM_ATTEMPTS").getName(), Operator.LT , 10 ));
				if (lastId != null){
					where.add(new Expression(ref.getColumnDescriptor("ID").getName(),Operator.GT,lastId));
				}
				
				db = Database.getInstance();
				Select select = new Select().from(DelayedTask.class).
						where(where).
						orderBy(DelayedTask.DEFAULT_ORDER_BY_COLUMNS);
				List<DelayedTask> jobs = select.execute(DelayedTask.class,100);
				
				manager.addDelayedTasks(jobs);
				db.getCurrentTransaction().commit();
			}finally{
				if (db != null){
					db.close();
				}
			}
		}

	}
}
