package com.venky.swf.db.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Select;

public class QueryCache {
	private Map<Expression,SortedSet<Record>> queryCache = new HashMap<Expression, SortedSet<Record>>();
	private final ModelReflector ref ;
	private final Table table;
	public QueryCache(String tableName){
		table = Database.getTable(tableName);
		ref = table.getReflector();
	}
	
	public SortedSet<Record> getCachedResult(Expression where){
		if (where != null && where.isEmpty()){
			where = null;
		}
		
		SortedSet<Record> result = queryCache.get(where);
		if (result == null && where != null){
			synchronized (queryCache) {
				result = queryCache.get(where);
				if (result == null && ref.isAnnotationPresent(CONFIGURATION.class)){
					SortedSet<Record> completeList = queryCache.get(null);
					if (completeList == null){
						completeList = new TreeSet<Record>();
						List<Model> models = new Select().from(table.getTableName()).execute();
						for (Model model:models){
							Record record = model.getRawRecord();
							completeList.add(record);
						}
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
	public SortedSet<Record> filter(Expression where,SortedSet<Record> completeList){
		SortedSet<Record> result = new TreeSet<Record>();
		if (where == null || where.isEmpty()){
			result.addAll(completeList);
			return result;
		}
		for (Record m:completeList){
			if (where.eval(m)){
				result.add(m);
			}
		}
		return result;
	}
	public void setCachedResult(Expression where, SortedSet<Record> result){
		if (where != null && where.isEmpty()){
			where = null;
		}
		queryCache.put(where, result);
	}
	
	public Record getCachedRecord(Record record){
		if (queryCache.containsKey(null)){
			SortedSet<Record> values = queryCache.get(null);
			if (values.contains(record)){
				return values.tailSet(record).first(); 
			}
			return null;
		}
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				SortedSet<Record> values = queryCache.get(cacheKey);
				if (values.contains(record)){
					return values.tailSet(record).first(); 
				}
			}
		}
		return null;
	}
	
	public <M extends Model> void add(M m){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(m)){
				SortedSet<Record> values = queryCache.get(cacheKey);
				values.add(m.getRawRecord());
			}
		}
	}
	public <M extends Model> void remove(M m){
		for (Expression cacheKey:queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(m)){
				SortedSet<Record> values = queryCache.get(cacheKey);
				values.remove(m.getRawRecord());
			}
		}
	}
	
	public void clear(){
		queryCache.clear();
	}
	
}
