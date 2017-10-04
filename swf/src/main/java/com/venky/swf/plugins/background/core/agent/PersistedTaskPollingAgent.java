package com.venky.swf.plugins.background.core.agent;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

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

		private int lastRecordId = -1 ;
		public PersistedTaskPoller(){
			
		}
		public PersistedTaskPoller(int lastRecordId) {
			this.lastRecordId = lastRecordId;
		}
		private int maxTasksToBuffer = 10;
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
				if (jobs.size() < getMaxTasksToBuffer()) { 
					jobs.add(new PersistedTaskPoller(lastRecordId));
				}else {
					jobs.add(new  AgentFinishUpTask(getAgentName()));
				}
			}

			return jobs;
		}

		@Override
		public String getAgentName() {
			return PERSISTED_TASK_POLLER;
		}

		@Override
		protected boolean isAgentTaskQPersistent() {
			return false;
		}

	}
	public static final String PERSISTED_TASK_POLLER = "PERSISTED_TASK_POLLER";

	@Override
	public AgentSeederTask createSeederTask() {
		return new PersistedTaskPoller();
	}


}
