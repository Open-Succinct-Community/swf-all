package com.venky.swf.plugins.datamart.agent.datamart;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.agent.AgentSeederTask;
import com.venky.swf.plugins.datamart.db.model.EtlRestart;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class ExtractorTask<M extends Model> extends AgentSeederTask{
	
	private static final long serialVersionUID = 242185599189303790L;

	EtlRestart restart = null;
	public EtlRestart getEtlRestart(){
		if (restart != null){
			return  restart;
		}
		synchronized (this) {
			if (restart != null){
				return  restart;
			}
		}
		Select s = new Select().from(EtlRestart.class);
		Expression where = new Expression(s.getPool(),Conjunction.AND);
		where.add(new Expression(s.getPool(),"AGENT_NAME",Operator.EQ, getAgentName()));
		s.where(where);
		List<EtlRestart> l = s.execute();
		if (l.isEmpty()) {
			restart = Database.getTable(EtlRestart.class).newRecord();
			restart.setAgentName(getAgentName());
			recordRestart(restart, null);
			restart.save();
		}else {
			restart = l.get(0);
		}
		return restart;	
	}
	
	public int getNumTasksToBuffer(){
		return 200;
	}
	@Override
	public List<Task> getTasks() {
		EtlRestart restart = getEtlRestart();
		List<M> l = new Select(getSelectedColumns()).from(getModelClass()).where(getWhereClause(restart)).groupBy(getGroupByColumns()).orderBy(getOrderByColumns()).execute(getNumTasksToBuffer());
		List<Task> tasks = new ArrayList<>();
		l.forEach(m ->{
			Task task = createTransformTask(m);
			if (task != null) {
				tasks.add(task);
			}
			recordRestart(restart, m);
		});
		if (getNumTasksToBuffer() == 0 || l.isEmpty()  || l.size() < getNumTasksToBuffer()){
			tasks.add(getFinishUpTask());
		}else {
			tasks.add(this);
		}
		restart.save();
		return tasks;
	}

	public String[] getOrderByColumns(){
		return getRestartFields();
	}


	protected String[] getSelectedColumns() {
		return new String[] {};
	}
	
	protected String[] getGroupByColumns() { 
		return null;
	}

	protected String[] getRestartFields(){
		return new String[]{"UPDATED_AT"};
	}
	protected Expression getWhereClause(EtlRestart restart){
		String[] fields = getRestartFields();
		List<String> values = new LinkedList<>();
		for (int i = 0 ; i < fields.length ; i ++ ){
			values.add(null);
		}
		String restartValue = restart.getRestartFieldValue();

		StringTokenizer tokenizer = new StringTokenizer(restartValue == null ?  " " : restartValue, "|");
		int iToken = 0;
		while(tokenizer.hasMoreTokens()) {
			values.set(iToken,tokenizer.nextToken());
			iToken++;
		}


		ModelReflector<M> ref =   getReflector();
		Expression where = new Expression(ref.getPool(),Conjunction.OR);
		for (int i = 0 ; i < fields.length ; i++ ){
			String gtF = fields[i];
			Expression part = new Expression(ref.getPool(),Conjunction.AND);
			for (int j = 0 ; j < i  ; j ++){
				String f = fields[j];
				Object value = valueOf(fields[j],values.get(j));
				if (value == null){
					part.add(new Expression(ref.getPool(),f, Operator.EQ, getReflector().getFieldGetter(f).getReturnType().cast(value)));
				}else {
					part.add(new Expression(ref.getPool(),f, Operator.EQ, value));
				}
			}

			Object value = valueOf(fields[i],values.get(i));
			if (value != null){
				part.add(new Expression(ref.getPool(),gtF, Operator.GT, value));
			}else {
				part.add(new Expression(ref.getPool(),gtF, Operator.NE));
			}
			where.add(part);
		}

		Expression finalWhere = new Expression(ref.getPool(),Conjunction.AND); // So that child classes can add more filters.
		finalWhere.add(where);
		return finalWhere;
	}
	protected <T> String toString(String field, T value){
		return Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(
				getReflector().getFieldGetter(field).getReturnType()).getTypeConverter().toString(value);

	}
	protected <T> T valueOf(String field, Object value){
		return (T)Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(
				getReflector().getFieldGetter(field).getReturnType()).getTypeConverter().valueOf(value);

	}
	public void recordRestart(EtlRestart restart, M m){
		StringBuilder restartFieldName = new StringBuilder();
		StringBuilder restartFieldValue = new StringBuilder();
		String[] fields = getRestartFields();
		for (int i = 0 ; i < fields.length ; i ++ ){
			if (restartFieldName.length() > 0){
				restartFieldName.append("|");
			}
			restartFieldName.append(fields[i]);
			if (restartFieldValue.length() > 0){
				restartFieldValue.append("|");
			}
			restartFieldValue.append(toString(fields[i],getRestartValue(m,fields[i])));
		}
		restart.setRestartFieldName(restartFieldName.toString());
		restart.setRestartFieldValue(restartFieldValue.toString());
	}

	protected Object getRestartValue(M m , String field){
		Object restartFieldValue = null ;
		if (m != null){
			Object restartValue = getReflector().get(m, field);
			restartFieldValue =  valueOf(field,restartValue);
		}
		return restartFieldValue;
	}

	
	protected abstract Task createTransformTask(M m) ;


	protected ModelReflector<M> getReflector(){
		return ModelReflector.instance(getModelClass());
	}
	
	@SuppressWarnings("unchecked")
	protected Class<M> getModelClass(){
		ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
		return (Class<M>) pt.getActualTypeArguments()[0];
	}
}
