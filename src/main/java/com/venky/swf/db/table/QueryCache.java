package com.venky.swf.db.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Select;

public class QueryCache<M extends Model> {
	private Map<Expression,List<M>> queryCache = new HashMap<Expression, List<M>>();
	private Class<M> modelClass = null;
	public QueryCache(Class<M> modelClass){
		this.modelClass = modelClass;
	}
	public List<M> getCachedResult(Expression where){
		if (where != null && where.isEmpty()){
			where = null;
		}
		List<M> result = queryCache.get(where);
		if (result == null && where != null){
			synchronized (queryCache) {
				result = queryCache.get(where);
				if (result == null && modelClass.isAnnotationPresent(CONFIGURATION.class)){
					List<M> completeList = queryCache.get(null);
					if (completeList == null){
						completeList = new Select().from(Database.getInstance().getTable(modelClass).getTableName()).execute();
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
	public List<M> filter(Expression where,List<M> completeList){
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
	public void setCachedResult(Expression where, List<M> result){
		if (where != null && where.isEmpty()){
			where = null;
		}
		queryCache.put(where, result);
	}
	
	public void add(M record){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				List<M> values = queryCache.get(cacheKey);
				if (!values.contains(record)){
					values.add(record);
				}
			}
		}
		//TODO Propagate Cache to all JVM Threads and other JVMS.
	}
	public void remove(M record){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				List<M> values = queryCache.get(cacheKey);
				values.remove(record);
			}
		}
		//TODO Propagate Cache to all JVM Threads and other JVMS.
	}
	
}
