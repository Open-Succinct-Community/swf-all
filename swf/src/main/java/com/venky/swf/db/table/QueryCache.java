package com.venky.swf.db.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;

public class QueryCache {
	private SortedSet<Record> cachedRecords = new TreeSet<Record>();

	private Map<Expression, SortedSet<Record>> queryCache = new HashMap<Expression, SortedSet<Record>>();

	private final Table table;

	public Table getTable() {
		return table;
	}

	public QueryCache(String tableName) {
		table = Database.getTable(tableName);
	}

	public SortedSet<Record> getCachedResult(Expression where, int maxRecords) {
		Timer timer = Timer.startTimer();
		
		try {
			if (where != null && where.isEmpty()) {
				where = null;
			}
	
			SortedSet<Record> result = queryCache.get(where);
			if (result == null) {
				synchronized (queryCache) {
					result = queryCache.get(where);
					if (result == null && maxRecords > 0
							&& cachedRecords.size() > maxRecords) {
						SortedSet<Record> tmpResult = new TreeSet<Record>();
						filter(where, cachedRecords, tmpResult, maxRecords);
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

	private void filter(Expression where, SortedSet<Record> source,
			SortedSet<Record> target, int maxRecords) {
		for (Iterator<Record> i = source.iterator(); i.hasNext()
				&& target.size() < maxRecords;) {
			Record m = i.next();
			if (where == null || where.isEmpty() || where.eval(m)) {
				target.add(m);
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

	public void setCachedResult(Expression where, SortedSet<Record> result) {
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
			setCachedResult(getIdWhereClause(record),new TreeSet<Record>(Arrays.asList(record)));
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
					SortedSet<Record> values = queryCache.get(cacheKey);
					values.add(record);
				}
			}
		}
	}
	public void registerUpdate(Record record) {
		for (Expression cacheKey: queryCache.keySet()){
			if (cacheKey == null || cacheKey.eval(record)){
				queryCache.get(cacheKey).add(record);
			}else {
				queryCache.get(cacheKey).remove(record);
			}
		}
		
	}
	public void registerDestroy(Record record) {
		if (remove(record)) {
			for (Expression cacheKey : queryCache.keySet()) {
				if (cacheKey == null || cacheKey.eval(record)) {
					SortedSet<Record> values = queryCache.get(cacheKey);
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

}
