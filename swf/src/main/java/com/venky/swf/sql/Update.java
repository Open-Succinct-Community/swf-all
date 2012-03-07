package com.venky.swf.sql;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.BindVariable;

public class Update extends DataManupulationStatement{
	private String table = null;
	private Map<String,BindVariable> values = new HashMap<String, BindVariable>();
	private Map<String,String> unBoundedValues = new HashMap<String, String>();
	private SortedSet<String> keys = new TreeSet<String>();
	public Update(Class<? extends Model> modelClass){
		this(Database.getInstance().getTable(modelClass).getRealTableName());
	}
	private Update(String table){
		this.table = table;
	}
	public Update setUnBounded(String name,String value){
		assert !values.containsKey(name);
		unBoundedValues.put(name, value);
		keys.add(name);
		return this;
	}
	public Update set(String name,BindVariable value){
		assert !unBoundedValues.containsKey(name);
		values.put(name, value);
		keys.add(name);
		return this;
	}
	public Update set(Map<String,BindVariable> values){
		for (String key : values.keySet()){
			set(key,values.get(key));
		}
		return this;
	}


	@Override
	protected void finalizeParameterizedSQL() {
		StringBuilder builder = getQuery();
		builder.append("UPDATE ").append(table);
		builder.append(" SET ");
		Iterator<String> ki = keys.iterator();
		while (ki.hasNext()){
			String key = ki.next();
			if (unBoundedValues.containsKey(key)){
				builder.append(key).append(" = ").append(unBoundedValues.get(key));
			}else {
				builder.append(key).append(" = ? " );
				getValues().add(values.get(key));
			}
			if (ki.hasNext()){
				builder.append(",");
			}
		}
		Expression where = getWhereExpression();
		if (where != null){
			builder.append(" WHERE ");
			builder.append(where.getParameterizedSQL());
			getValues().addAll(where.getValues());
		}
	}

	private Expression whereExpression ;
	public Update where(Expression expression){
		this.whereExpression = expression;
		return this;
	}
	
	public Expression getWhereExpression(){
		return whereExpression;
	}

}
