package com.venky.swf.db.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;

public class QueryCache {
	private SortedSet<Record> cachedRecords = new TreeSet<Record>();
	private Map<Expression, Set<Record>> queryCache = new HashMap<Expression, Set<Record>>();
	private final Table table;

	public Table getTable() {
		return table;
	}
	public boolean isEmpty(){
		return cachedRecords.isEmpty();
	}
	public QueryCache(String tableName) {
		this(Database.getTable(tableName));
	}
	private QueryCache(Table table){
		this.table = table;
	}

	public Set<Record> getCachedResult(Expression where, int maxRecords, boolean locked) {
		Timer timer = Timer.startTimer();
		
		try {
			if (where != null && where.isEmpty()) {
				where = null;
			}
	
			Set<Record> result = queryCache.get(where);
			if (result == null) {
				synchronized (queryCache) {
					result = queryCache.get(where);
					if (result == null && maxRecords > 0 && cachedRecords.size() > maxRecords) {
						Set<Record> tmpResult = new SequenceSet<Record>();
						filter(where, tmpResult, maxRecords,locked);
						if (tmpResult.size() >= maxRecords) {
							result = tmpResult;
						}
					}
				}
			}
			return result;
		}finally{
			timer.stop();
		}
	}

	private void filter(Expression where, Set<Record> target, int maxRecords,boolean locked) {
		for (Iterator<Record> i = cachedRecords.iterator(); i.hasNext() && target.size() < maxRecords;) {
			Record m = i.next();
			if (where == null || where.isEmpty() || where.eval(m)) {
				if (!locked || (locked == m.isLocked())){
					target.add(m);
				}
			}
		}
	}

	public Record getCachedRecord(Record record) {
		SortedSet<Record> tail = cachedRecords.tailSet(record);
		if (tail == null || tail.isEmpty()) {
			return null;
		}
		Record recordInCache = tail.first();
		if (recordInCache.compareTo(record) == 0) {
			return recordInCache;
		}
		return null;
	}

	public void setCachedResult(Expression where, Set<Record> result) {
		if (where != null && where.isEmpty()) {
			where = null;
		}
		if (!queryCache.containsKey(where)){
			queryCache.put(where, result);
		}
	}

	private Expression getIdWhereClause(int id) {
		return new Expression("ID", Operator.EQ, id);
	}

	private Expression getIdWhereClause(Record record) {
		if (record != null) {
			Integer id = record.getId();
			if (id != null) {
				return getIdWhereClause(id);
			}
		}
		throw new NullPointerException("Record doesnot have ID !");
	}

	public boolean add(Record record) {
		boolean ret = cachedRecords.add(record);
		if (ret){
			setCachedResult(getIdWhereClause(record),new HashSet<Record>(Arrays.asList(record)));
		}
		return ret;
	}

	public boolean remove(Record record) {
		boolean ret = cachedRecords.remove(record);
		if (ret) {
			queryCache.remove(getIdWhereClause(record));
		}
		return ret;
	}

	public void registerInsert(Record record) {
		if (add(record)) {
			for (Expression cacheKey : queryCache.keySet()) {
				if (cacheKey == null || cacheKey.eval(record)) {
					Set<Record> values = queryCache.get(cacheKey);
					values.add(record);
				}
			}
		}
	}
	public void registerUpdate(Record record) {
		remove(record);
		for (Expression cacheKey: queryCache.keySet()){
			Set<Record> keyedRecords = queryCache.get(cacheKey);
			keyedRecords.remove(record);
			if (cacheKey == null || cacheKey.eval(record)){
				keyedRecords.add(record);
			}
		}
		add(record);
	}
	public void registerDestroy(Record record) {
		if (remove(record)) {
			for (Expression cacheKey : queryCache.keySet()) {
				if (cacheKey == null || cacheKey.eval(record)) {
					Set<Record> values = queryCache.get(cacheKey);
					values.remove(record);
				}
			}
		}
	}

	// Called from db record insert.
	public <M extends Model> void registerInsert(M m) {
		registerInsert(m.getRawRecord());
	}

	public <M extends Model> void registerUpdate(M m) {
		registerUpdate(m.getRawRecord());
	}

	// Called from db record destroy.
	public <M extends Model> void registerDestroy(M m) {
		registerDestroy(m.getRawRecord());
	}

	public void clear() {
		queryCache.clear();
	}
	
	public void merge(QueryCache completedTransactionCache) {
		Set<Record> mergedRecords = new HashSet<Record>();
		
		for (Expression exp: completedTransactionCache.queryCache.keySet()){
			Set<Record> recentRecords = completedTransactionCache.queryCache.get(exp);
			if (queryCache.containsKey(exp)){
				Set<Record> oldRecords = queryCache.get(exp);
				oldRecords.removeAll(recentRecords);
				if (!oldRecords.isEmpty()){
					//There have got removed. 
					for (Record record:oldRecords){
						registerDestroy(record);
					}
				}
			}
			for (Record record:recentRecords){
				if (!mergedRecords.contains(record)){
					registerUpdate(record);
					mergedRecords.add(record);
				}
			}
			queryCache.put(exp, recentRecords);
		}
	}
	
	public QueryCache copy(){
		QueryCache cache = new QueryCache(table);
		cache.cachedRecords.addAll(cachedRecords); 
		cache.queryCache.putAll(queryCache);
		return cache;
	}
}
