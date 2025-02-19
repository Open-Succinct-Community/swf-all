package com.venky.swf.db.table;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import com.venky.core.checkpoint.Mergeable;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class QueryCache implements Mergeable<QueryCache> , Cloneable{
	private NavigableSet<Record> cachedRecords = Collections.synchronizedNavigableSet(new TreeSet<Record>());
	private HashMap<Expression, SequenceSet<Record>> queryCache = new HashMap<Expression, SequenceSet<Record>>();
	private Table<? extends Model> table;
	private ModelReflector<? extends Model> reflector ;
	private String loggerName = null;
	
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
		if (this.table != null){
			this.reflector = this.table.getReflector();
		}
		this.loggerName = QueryCache.class.getName() + "." + table.getModelClass().getSimpleName();
	}
	public void registerLockRelease(){
		if (hasLockedRecords){
			synchronized (cachedRecords) {
				for (Record record : cachedRecords) {
					record.setLocked(false);
				}
			}
			hasLockedRecords = false;
		}
	}
	@SuppressWarnings("unchecked")
	public QueryCache clone(){
		try {
			QueryCache clone = (QueryCache) super.clone();
			clone.cachedRecords = Collections.unmodifiableNavigableSet(new TreeSet<Record>(cachedRecords));
			clone.queryCache = new HashMap<>(queryCache);
			ObjectUtil.cloneValues(clone.cachedRecords);
			ObjectUtil.cloneValues(clone.queryCache);
			return clone;
		}catch (CloneNotSupportedException ex){
			throw new RuntimeException(ex);
		}
	}
	private static final Level defaultLevel = Level.FINE;

	public boolean isIdWhereClause(Expression where){
	    if (where != null && where.getNumChildExpressions() == 0){
            if (where.getOperator() == Operator.EQ && "ID".equalsIgnoreCase(where.getColumnName())){
                return where.getValues().size() == 1;
            }
        }
        return false;
    }
	public SequenceSet<Record> getCachedResult(Expression where, int maxRecords, boolean locked) {
		SWFLogger cat = Config.instance().getLogger(loggerName);
		Timer timer = cat.startTimer(null,Config.instance().isTimerAdditive());
		StringBuilder debug = new StringBuilder();
		try {
			if (where != null && where.isEmpty()) {
				where = null;
			}
			boolean requireFilteringForLockedRecords = true; 
			String queryCriteria = (where == null ? "null" : where.getRealSQL());

			SequenceSet<Record> result = queryCache.get(where);
			
			if (cat.isLoggable(defaultLevel) && result != null ){
				debug.append("Cache for " + getTable().getRealTableName() + " has criteria:" + queryCriteria);
			}
			
			
			if (result == null) {
				synchronized (queryCache) {
					result = queryCache.get(where);
					if (cat.isLoggable(defaultLevel) && result != null ){
						debug.append("Cache for " + getTable().getRealTableName() + " has criteria:" + queryCriteria);
					}

					boolean fullTableScanPerformed = queryCache.containsKey(null);
					if (result == null && !isIdWhereClause(where)){
						if (cat.isLoggable(Level.FINER)){
							debug.append("Cache for " + getTable().getRealTableName() + " does not have criteria:" + queryCriteria);
							debug.append("\nChecking against available cachedRecords if there are enough records satifying the criteria");
							debug.append("\nWas full table scanned ever?:" + fullTableScanPerformed);
						}
						if (fullTableScanPerformed) {
							result = new SequenceSet<Record>();
							filter(cachedRecords,where, result, Select.MAX_RECORDS_ALL_RECORDS,false);
							setCachedResult(where, result);
						}else if (maxRecords > 0 ){
							SequenceSet<Record> tmpResult = new SequenceSet<Record>();
							filter(cachedRecords,where, tmpResult, maxRecords,locked);
							if (tmpResult.size() >= maxRecords){
								result = tmpResult; 
								requireFilteringForLockedRecords = false;
							}
						}
					}
				}
			}
			
			if (result != null && locked &&  requireFilteringForLockedRecords){
				debug.append(" Checking for locked records from cache!");
				SequenceSet<Record> tmpResult = new SequenceSet<Record>();
				filter(result, null, tmpResult, maxRecords, locked);
				if (tmpResult.size() < result.size()){
					result = null;
					if (maxRecords > 0 && tmpResult.size() >= maxRecords){
						result = tmpResult;
					}
				}
			}
			
			if (cat.isLoggable(defaultLevel)){
				if (result == null || result.isEmpty()) {
					debug.append("NOT ");
				}
				debug.append("Enough " + (locked ? "locked" : "" ) + "records found in cache.");
				cat.log(defaultLevel,debug.toString());
			}
			
			
			
			return result;
		}finally{
			timer.stop();
		}
	}
	
	private void filter( Set<Record> cachedRecords, Expression where, Set<Record> target, int maxRecords,boolean locked) {
		synchronized (cachedRecords) {
			for (Iterator<Record> i = cachedRecords.iterator(); i.hasNext() && (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || target.size() < maxRecords); ) {
				Record m = i.next();
				if (where == null || where.isEmpty() || where.eval(m, reflector)) {
					if (!locked || m.isLocked()) {
						target.add(m);
					} else if (maxRecords == Select.MAX_RECORDS_ALL_RECORDS) {
						// m is not locked.
						target.clear();
						return;
					}
				}
			}
		}
	}

	public Record getCachedRecord(Record record) {
		
		SortedSet<Record> tail = cachedRecords.tailSet(record);
		if (tail.isEmpty()) {
			return null;
		}
		Record recordInCache = tail.first();
		if (recordInCache.compareTo(record) == 0) {
			return recordInCache;
		}
		return null;
	}

	public void setCachedResult(Expression where, SequenceSet<Record> result) {
		if (where != null && where.isEmpty()) {
			where = null;
		}
		synchronized (queryCache) {
			if (!queryCache.containsKey(where) && result != null) {
				queryCache.put(where, result);
				if (where == null) {
					for (Record record : result) {
						for (String column : getTable().getReflector().getIndexedColumns()) {
							Expression indexWhere = getIndexWhereClause(record, column);
							SequenceSet<Record> records = queryCache.get(indexWhere);
							if (records == null) {
								records = new SequenceSet<Record>();
								queryCache.put(indexWhere, records);
							}
							
							records.add(record);
						}
					}
				}
			} else if (queryCache.containsKey(where) && result == null) {
				queryCache.remove(where);
			}
		}
	}

	private Expression getIdWhereClause(long id) {
		return new Expression(getTable().getReflector().getPool(),"ID", Operator.EQ, id);
	}

	private Expression getIdWhereClause(Record record) {
		if (record != null) {
			Long id = record.getId();
			if (id != null) {
				return getIdWhereClause(id);
			}
		}
		throw new NullPointerException("Record doesnot have ID !");
	}
	private Expression getIndexWhereClause(Record record, String column) {
		Object value = record.get(column);
		if (value != null){
			return new Expression(getTable().getReflector().getPool(),column,Operator.EQ,value);
		}else {
			return new Expression(getTable().getReflector().getPool(),column,Operator.EQ);
		}
	}
	private boolean hasLockedRecords = false; 
	public boolean add(Record record) {
		boolean ret = cachedRecords.add(record);
		if (ret){
			SequenceSet<Record> set = new SequenceSet<Record>();
			set.add(record);
			setCachedResult(getIdWhereClause(record),set);
			/*
			 * This itself is a performance hog and not need most of the time.
			for (Expression ukCondition: getTable().getReflector().getUniqueKeyConditions(record)){
				setCachedResult(ukCondition, set);
			}*/
		}
		hasLockedRecords = hasLockedRecords || record.isLocked();
		return ret;
	}

	public boolean remove(Record record) {
		boolean ret = cachedRecords.remove(record);
		ret = ret || (queryCache.remove(getIdWhereClause(record)) != null);
		return ret;
	}

	public void registerInsert(Record record) {
		if (add(record)) {
			synchronized (queryCache){
				for (Expression cacheKey : queryCache.keySet()) {
					if (cacheKey == null || cacheKey.eval(record,reflector)) {
						Set<Record> values = queryCache.get(cacheKey);
						values.add(record);
					}
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
		synchronized (queryCache) {
			for (Expression cacheKey : queryCache.keySet()) {
				if (recordInCache == null) {
					if (cacheKey == null || cacheKey.eval(record, reflector)) {
						queryCache.get(cacheKey).add(record); // Will not be added if already exists.
					}
				} else if (cacheKey != null && !isIdWhereClause(cacheKey)) {
					if (!cacheKey.eval(record, reflector)) {
						queryCache.get(cacheKey).remove(record); // Will not be removed if it doesnot exist.
					} else {
						queryCache.get(cacheKey).add(record);
					}
				}
			}
		}
		if (recordInCache == null){
			add(record);
		}
		return record;
	}
	public void registerDestroy(Record record) {
		if (remove(record)) {
			synchronized (queryCache) {
				for (Expression cacheKey : queryCache.keySet()) {
					if (cacheKey == null || cacheKey.eval(record, reflector)) {
						Set<Record> values = queryCache.get(cacheKey);
						values.remove(record);
					}
				}
			}
		}
	}

	// Called from db record insert.
	public <M extends Model> void registerInsert(M m) {
		registerInsert(m.getRawRecord());
	}

	public <M extends Model> void registerUpdate(M m) {
		m.setRawRecord(registerUpdate(m.getRawRecord()));
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
			SequenceSet<Record> currentRecords = new SequenceSet<Record>();
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
