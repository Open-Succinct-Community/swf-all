package com.venky.swf.plugins.background.core.agent;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.CompositeTask;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.ArrayList;
import java.util.List;

public class PersistedTaskPollingAgent implements AgentSeederTaskBuilder  {

	public static class PersistedTaskPoller extends AgentSeederTask {

		private static final long serialVersionUID = 512886938185460373L;
		private final ModelReflector<DelayedTask> ref = ModelReflector.instance(DelayedTask.class);
		
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

		private long lastRecordId = -1 ;
		public PersistedTaskPoller(){
			
		}

		@Override
		public Priority getTaskPriority() {
			return Priority.LOW;
		}

		public PersistedTaskPoller(long lastRecordId) {
			this.lastRecordId = lastRecordId;
		}
		private int maxTasksToBuffer = Config.instance().getIntProperty("swf.persisted.task.polling.batch.size",1000);
		public int getMaxTasksToBuffer(){ 
			return maxTasksToBuffer;
		}
		
		@Override
		public List<Task> getTasks() {
	        DelayedTask lastRecord = lastRecordId > 0 ? Database.getTable(DelayedTask.class).get(lastRecordId) : null ;
			
			Expression where = new Expression(ref.getPool(),Conjunction.AND);
			where.add(new Expression(ref.getPool(),ref.getColumnDescriptor("NUM_ATTEMPTS").getName(), Operator.LT , 10 ));
			if (lastRecord != null){
				where.add(getWhereClause(lastRecord));
			}
			
			List<String> realColumns = ref.getRealColumns();
			realColumns.remove("DATA");
			realColumns.remove("LAST_ERROR"); //Remove Clob and Blob Columns.
			Select select = new Select(realColumns.toArray(new String[]{})).from(DelayedTask.class).
					where(where).
					orderBy(DelayedTask.DEFAULT_ORDER_BY_COLUMNS);
			
			List<Task> jobs = new ArrayList<>();
			
			select.execute(DelayedTask.class,getMaxTasksToBuffer(),new Select.ResultFilter<DelayedTask>() {
				@Override
				public boolean pass(DelayedTask record) {
					jobs.add(record);
					lastRecordId = record.getId();
					return false;
				}
			});
			Config.instance().getLogger(getClass().getName()).finest("Number of tasks found:" + jobs.size());

			if (!jobs.isEmpty()){
			    Task lastTask = jobs.remove(jobs.size()-1);
                jobs.add(new CompositeTask(new PersistedTaskPoller(lastRecordId),lastTask) {
                    @Override
                    public void execute(){
                        try {
                            super.execute();
                        }catch (Exception ex){
                            finish(); // Don't leave Agent in bad state of Running and having no Seeder!.
                        }
                    }
                });
                //First do get jobs then the task or else the task would be removed and then we will do full select again.
			}

			return jobs;
		}

		@Override
		public String getAgentName() {
			return PERSISTED_TASK_POLLER;
		}

	}
	public static final String PERSISTED_TASK_POLLER = "PERSISTED_TASK_POLLER";

	@Override
	public AgentSeederTask createSeederTask() {
		return new PersistedTaskPoller();
	}


}
