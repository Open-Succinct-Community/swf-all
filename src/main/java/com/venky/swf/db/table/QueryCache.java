package com.venky.swf.db.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.swf.db.model.Model;

public class QueryCache<M extends Model> {
	private Map<String,List<M>> queryCache = new HashMap<String, List<M>>();
	
	public List<M> getCachedResult(String query){
		return queryCache.get(query);
	}
	
	public void setCachedResult(String query, List<M> result){
		queryCache.put(query, result);
	}
	
}
