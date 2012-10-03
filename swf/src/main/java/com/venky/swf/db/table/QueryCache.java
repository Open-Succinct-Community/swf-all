package com.venky.swf.db.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.core.checkpoint.Mergeable;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class QueryCache implements Mergeable<QueryCache> , Cloneable{
	private TreeSet<Record> cachedRecords = new TreeSet<Record>();
	private HashMap<Expression, Set<Record>> queryCache = new HashMap<Expression, Set<Record>>();
	private Table<? extends Model> table;

	public Table<? extends Model> getTable() {
		return table;
	}
	public boolean isEmpty(){
		return cachedRecords.isEmpty();
	}
	public QueryCache(String tableName) {
		this(Database.getTable(tableName));
	}
	private <M extends Model> QueryCache(Table<M> table){
		this.table = table;
	}
	public void registerLockRelease(){
		if (hasLockedRecords){
			for (Record record: cachedRecords){
				record.setLocked(false);
			}
			hasLockedRecords = false;
		}
	}
	@SuppressWarnings("unchecked")
	public QueryCache clone(){
		try {
			QueryCache clone = (QueryCache) super.clone();
			clone.cachedRecords = (TreeSet<Record>) cachedRecords.clone();
			clone.queryCache = (HashMap<Expression, Set<Record>>) queryCache.clone();
			
			ObjectUtil.cloneValues(clone.cachedRecords);
			ObjectUtil.cloneValues(clone.queryCache);
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		
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
					boolean fullTableRecordAvailable = queryCache.containsKey(null);
					if (result == null && (maxRecords > 0 && cachedRecords.size() > maxRecords) || (fullTableRecordAvailable && !locked)) {
						Set<Record> tmpResult = new SequenceSet<Record>();
						filter(cachedRecords,where, tmpResult, maxRecords,locked);
						if (tmpResult.size() >= maxRecords) {
							result = tmpResult;
						}
					}
				}
			}else if (locked){
				Set<Record> tmpResult = new SequenceSet<Record>();
				filter(result, null, tmpResult, Select.MAX_RECORDS_ALL_RECORDS, locked);
				if (tmpResult.size() < result.size()){
					result = null;
				}
			}
			return result;
		}finally{
			timer.stop();
		}
	}

	private void filter( Set<Record> cachedRecords, Expression where, Set<Record> target, int maxRecords,boolean locked) {
		for (Iterator<Record> i = cachedRecords.iterator(); i.hasNext() && (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || target.size() < maxRecords) ;) {
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
			if (where == null){
				for (Record record:result){
					for (String column: getTable().getReflector().getIndexedColumns()) {
						Expression indexWhere = getIndexWhereClause(record,column);
						Set<Record> records = queryCache.get(indexWhere); 
						if (records == null){
							records = new SequenceSet<Record>();
							queryCache.put(indexWhere, records);
						}
						
						records.add(record);
					}
				}
			}
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
	private Expression getIndexWhereClause(Record record, String column) {
		Object value = record.get(column);
		if (value != null){
			return new Expression(column,Operator.EQ,value);
		}else {
			return new Expression(column,Operator.EQ);
		}
	}
	private boolean hasLockedRecords = false; 
	public boolean add(Record record) {
		boolean ret = cachedRecords.add(record);
		if (ret){
			setCachedResult(getIdWhereClause(record),new HashSet<Record>(Arrays.asList(record)));
		}
		hasLockedRecords = hasLockedRecords || record.isLocked();
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
	
	public Record registerUpdate(Record updatedRecord) {
		Record recordInCache = getCachedRecord(updatedRecord);

		Record record = null;
		if (recordInCache != null){
			if (recordInCache != updatedRecord){ // No need to merge is the updatedRecord is already an object in the cache.
				recordInCache.merge(updatedRecord);// Will get used only in nested transactions.
			}
			record = recordInCache;
		}else {
			record = updatedRecord;
		}
		for (Expression cacheKey: queryCache.keySet()){
			Set<Record> keyedRecords = queryCache.get(cacheKey);
			if (cacheKey == null || cacheKey.eval(record)){
				keyedRecords.add(record); // Will not be added if already exists.
			}else {
				keyedRecords.remove(record);
			}
		}
		if (recordInCache == null){
			add(record);
		}
		return record;
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
		cachedRecords.clear();
	}
	
	public void merge(QueryCache completedTransactionCache) {
		Map<Record,Record> mergedRecords = new HashMap<Record,Record>();
		
		for (Expression exp: completedTransactionCache.queryCache.keySet()){
			Set<Record> recentRecords = completedTransactionCache.queryCache.get(exp);
			
			if (queryCache.containsKey(exp)){
				Set<Record> oldRecords = new HashSet<Record>(queryCache.get(exp));
				for (Record old:oldRecords){ 
					if (!recentRecords.contains(old)){
						registerDestroy(old);
					}
				}
			}
			Set<Record> currentRecords = new HashSet<Record>();
			for (Record record:recentRecords){
				Record mergedRecord = mergedRecords.get(record);
				if (mergedRecord == null){
					mergedRecord = registerUpdate(record);
					mergedRecords.put(record,mergedRecord);
				}
				currentRecords.add(mergedRecord);
			}
			
			if (!queryCache.containsKey(exp)){
				queryCache.put(exp, currentRecords);
			}//else{ 
			// registerUpdate would have fixed the map value against exp.
			//}
		}
	}
	
	public QueryCache copy(){
		QueryCache cache = new QueryCache(table);
		cache.cachedRecords.addAll(cachedRecords); 
		cache.queryCache.putAll(queryCache);
		return cache;
	}
}
