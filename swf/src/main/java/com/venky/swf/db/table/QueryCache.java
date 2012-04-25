package com.venky.swf.db.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Select;

public class QueryCache {
	private Map<Expression,List<? extends Model>> queryCache = new HashMap<Expression, List<? extends Model>>();
	private final ModelReflector ref ;
	private final Table table;
	public QueryCache(String tableName){
		table = Database.getTable(tableName);
		ref = table.getReflector();
	}
	
	public <M extends Model> List<M> getCachedResult(Expression where){
		if (where != null && where.isEmpty()){
			where = null;
		}
		
		List<M> result = (List<M>)queryCache.get(where);
		if (result == null && where != null){
			synchronized (queryCache) {
				result = (List<M>)queryCache.get(where);
				if (result == null && ref.isAnnotationPresent(CONFIGURATION.class)){
					List<M> completeList = (List<M>)queryCache.get(null);
					if (completeList == null){
						completeList = new Select().from(table.getTableName()).execute();
					}
					if (completeList != null){
						result = filter(where,completeList);
						setCachedResult(where, result);
					}
				}
			}
		}
		return result;
	}
	public <M extends Model> List<M> filter(Expression where,List<M> completeList){
		List<M> result = new ArrayList<M>();
		if (where == null || where.isEmpty()){
			result.addAll(completeList);
			return result;
		}
		for (M m:completeList){
			if (where.eval(m)){
				result.add(m);
			}
		}
		return result;
	}
	public void setCachedResult(Expression where, List<? extends Model> result){
		if (where != null && where.isEmpty()){
			where = null;
		}
		queryCache.put(where, result);
	}
	
	public <M extends Model> void add(M record){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				List values = queryCache.get(cacheKey);
				if (!values.contains(record)){
					values.add(record);
				}
			}
		}
		//TODO Propagate Cache to all JVM Threads and other JVMS.
	}
	public <M extends Model> void remove(M record){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				List values = queryCache.get(cacheKey);
				values.remove(record);
			}
		}
		//TODO Propagate Cache to all JVM Threads and other JVMS.
	}
	
	public void clear(){
		queryCache.clear();
	}
	
}
