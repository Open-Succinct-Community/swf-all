package com.venky.swf.sql;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.SWFTimeoutException;
import com.venky.swf.routing.Config;

public class Select extends SqlStatement{
	
	private String[] columnNames = null;
	private String[] tableNames ;
	private String[] orderBy;
	private String[] groupBy;
	protected boolean wait = true;
	protected boolean lock = true;
	public Select(String... columnNames){
		this(false,columnNames);
	}
	public Select(boolean lock,String...columnNames){
		this(lock,true,columnNames);
	}
	public Select(boolean lock,boolean wait,String...columnNames){
		this.lock = lock;
		this.wait = wait;
		this.columnNames = columnNames;
	}

	private Set<String> pools = new SequenceSet<String>();
	public String getPool(){
		if (pools.size() == 0){
			throw new RuntimeException("Cannot determine db Pool");
		}else if (pools.size() > 1){
			throw new RuntimeException("Cannot select across db pools in a single select statement " + pools);
		}else{
			return  pools.iterator().next();
		}
	}
	public Select addPool(String pool){
		pools.add(pool);
		return this;
	}
	
	private SWFLogger cat = Config.instance().getLogger(getClass().getName());
	@SuppressWarnings("unchecked")
	public Select from(Class<?>... models){
		Timer timer = cat.startTimer();
		try {
			String[] tables = new String[models.length];
			for (int i = 0 ; i< models.length ; i++){
				if (!Model.class.isAssignableFrom(models[i])){
					continue;
				}
				ModelReflector<? extends Model> ref = ModelReflector.instance((Class<? extends Model>)models[i]);
				tables[i] = ref.getTableName();
				pools.add(ref.getPool());
			}
			return from(tables);
		}finally{
			timer.stop();
		}
	}
	
	public Select from(String... tables){
		this.tableNames = tables;
		return this;
	}
	
	
	public Select orderBy(String... columnNames){
		List<String> orderbyColumns = new ArrayList<String>();
		for (String columnName: columnNames){
			StringTokenizer tok = new StringTokenizer(columnName,",");
			while (tok.hasMoreTokens()){
				orderbyColumns.add(tok.nextToken());
			}
		}
		this.orderBy = orderbyColumns.toArray(new String[]{});
		if (orderBy.length == 0){
			orderBy = null;
		}
		return this;
	}
	
	public Select groupBy(String... columnNames){
		this.groupBy = columnNames;
		return this;
	}
	
	private String having = null;
	public Select having( String condition ){
		this.having = condition;
		return this;
	}
	
	private <M extends Model> boolean allOrderByColumnsAreReal(Class<M> modelClass){
		Timer timer = cat.startTimer();
		try {
			if (orderBy == null){
				return false;
			}
			ModelReflector<M> ref = ModelReflector.instance(modelClass);

            for (String anOrderBy : orderBy) {
                String[] split = splitOrderByColumn(anOrderBy);
                String column = split[0];
                String field = ref.getFieldName(column);
                if (field == null || ref.getColumnDescriptor(field).isVirtual()) {
                    return false;
                }
            }
			return true;
		}finally{
			timer.stop();
		}
	}
	
	protected void finalizeParameterizedSQL(){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		if (columnNames == null || columnNames.length == 0){
			builder.append(" * ");
		}else {
			addlist(builder, columnNames);
		}
		if (tableNames != null && tableNames.length > 0){
			builder.append(" FROM ");
			addlist(builder, tableNames);
		}
		builder.append(" ");
		
		Expression where = getWhereExpression();
		if (where != null && !where.isEmpty()){
			builder.append(" WHERE ");
			builder.append(where.getParameterizedSQL());
			getValues().addAll(0,where.getValues()); // To handle fragment Additions.
		}
		getQuery().insert(0, builder.toString()); // To handle any fragment additions.
		builder = getQuery();
		
		if (groupBy != null){
			builder.append(" GROUP BY ");
			addlist(builder, groupBy);
		}
		if (having != null) {
			builder.append( " HAVING "); 
			builder.append(having);
		}
		
		if (orderBy != null){
			builder.append(" ORDER BY ");
			addlist(builder, orderBy);
		}
		
		if (lock){
			builder.append(Database.getJdbcTypeHelper(getPool()).getForUpdateLiteral());
			if (!wait && Database.getJdbcTypeHelper(getPool()).isNoWaitSupported()){
				builder.append(Database.getJdbcTypeHelper(getPool()).getNoWaitLiteral());
			}
		}
		
	}
	private void addlist(StringBuilder builder,String...strings ){
		if (strings == null){
			return ;
		}
		for (int i = 0; i < strings.length ; i++){
			if (i != 0){
				builder.append(", ");
			}
			builder.append(ConnectionManager.instance().getEscapedWord(getPool(),strings[i]));
		}
		
	}
	
	public static final int MAX_RECORDS_ALL_RECORDS = 0; 
	
	public <M extends Model> List<M> execute(){
		return execute(MAX_RECORDS_ALL_RECORDS);
	}
	
	public <M extends Model> List<M> execute(int maxRecords){
		if (tableNames.length != 1){
			throw new UnsupportedOperationException("Query is a join.Don't know what Collection to return.");
		}
		Table<M> table = Database.getTable(tableNames[0]);
		return execute(table.getModelClass(),maxRecords);
	}
	
	public <M extends Model> List<M> execute(Class<M> modelInterface){
		return execute(modelInterface,MAX_RECORDS_ALL_RECORDS,lock,null);
	}
	public <M extends Model> List<M> execute(Class<M> modelInterface,ResultFilter<M> filter){
		return execute(modelInterface,MAX_RECORDS_ALL_RECORDS,lock,filter);
	}
	public <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords) {
		return execute(modelInterface,maxRecords,lock,null);
	}
	public <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,ResultFilter<M> filter){
		return execute(modelInterface,maxRecords,lock,filter);
	}
	public <M extends Model> List<M> execute(Class<M> modelClass, boolean returnDecrypted){
		return execute(modelClass,MAX_RECORDS_ALL_RECORDS,lock,null,returnDecrypted);
	}


	protected boolean isCacheable(ModelReflector<? extends Model> ref){
		return (columnNames == null || columnNames.length == 0) && (ref.getRealModelClass() != null) ;
	}
	
	private String[] splitOrderByColumn(String orderBy){
		StringTokenizer tok = new StringTokenizer(orderBy);
		String columnName = tok.nextToken();
		String orderByType = "ASC";
		if (tok.hasMoreTokens()){
			orderByType =  tok.nextToken();
		}
		return new String[]{columnName,orderByType};
	}
	protected <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,boolean locked,ResultFilter<M> filter) {
		return execute(modelInterface,maxRecords,locked,filter,true);
	}
	protected <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,boolean locked,ResultFilter<M> filter, boolean returnDecrypted) {
		final String[] orderByPassed = this.orderBy;
		
		boolean sortResults = true; 
		if (this.orderBy != null && !allOrderByColumnsAreReal(modelInterface)){
			this.orderBy = null;
		}
		
        PreparedStatement st = null;
        try {
        	final ModelReflector<M> ref = ModelReflector.instance(modelInterface);
        	SequenceSet<Record> result = null;
        	List<M> ret = null;
        	QueryCache cache = Database.getInstance().getCache(ref);
			if (isCacheable(ref)){
        		result = cache.getCachedResult(getWhereExpression(),(orderByPassed != null ? Select.MAX_RECORDS_ALL_RECORDS :maxRecords),locked);
        	}else if (ref.getRealModelClass() == null && cache.isIdWhereClause(getWhereExpression())){
        		result = cache.getCachedResult(getWhereExpression(),(orderByPassed != null ? Select.MAX_RECORDS_ALL_RECORDS :maxRecords),locked);
        		//Return if cached manually.
			}
        	
        	if (result == null){
	            Timer queryTimer = cat.startTimer(getRealSQL());
	            Config.instance().getLogger(getClass().getName()).fine(getRealSQL());
	            try {
		            st = prepare();
		            if (maxRecords != Select.MAX_RECORDS_ALL_RECORDS ) {
		            	//st.setMaxRows(maxRecords + 1);
		            	st.setFetchSize(Math.min(maxRecords+1,10000));
		            }
		            if (this.orderBy != null){
		            	sortResults = false;
		            }
	            	if (!wait && 
	            			(!lock || (lock && !Database.getJdbcTypeHelper(getPool()).isNoWaitSupported())) && 
	            			Database.getJdbcTypeHelper(getPool()).isQueryTimeoutSupported()){
	            		Config.instance().getLogger(getClass().getName()).fine("Setting Statement Time out");
	            		st.setQueryTimeout(Config.instance().getIntProperty("swf.sql.Select.nowait.TimeOut",60));
	            	}
		            result = new SequenceSet<Record>();
		        	ret = new ArrayList<M>();
		            if (st.execute()){
		                ResultSet rs = st.getResultSet();
		                while (rs.next() && (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || ret.size() < maxRecords + 1)){
		                    Record r = new Record(getPool());
		                    r.load(rs,ref,returnDecrypted);
		                    r.setLocked(locked);

		                    if (isCacheable(ref)){
			                    Record cachedRecord = cache.getCachedRecord(r);
			                    if (cachedRecord != null ){
			                    	if (!locked || locked == cachedRecord.isLocked()){
			                    		r = cachedRecord;
			                    	}else {
			                    		r = cache.registerUpdate(r);
			                    	}
			                    }else {
			                    	cache.add(r);
			                    }
                                result.add(r);
		                    }
		                    M m = r.getAsProxy(modelInterface);
		                    if (filter == null || filter.pass(m)){
		                    	ret.add(m);
		                    }
		                }
		                rs.close();
		            }
		            if (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || ret.size() <= maxRecords){ // We are requesting maxRecords + 1;!
		            	if (isCacheable(ref)){
		            		cache.setCachedResult(getWhereExpression(), result);
		            	}
		            }else {
		            	ret.remove(ret.size()-1); // Remove the last extra one.!!
		            }
	            }catch (SQLException ex){
	            	if (Database.getJdbcTypeHelper(getPool()).isQueryTimeoutException(ex)){
	            		throw new SWFTimeoutException(ex);
	            	}else {
	            		throw ex;
	            	}
	            }finally{
	            	queryTimer.stop();
	            }
        	}else {
        		if (sortResults && orderByPassed != null && orderByPassed.length > 0){
        			Timer sorting = cat.startTimer("Sorting cached records");
            		Collections.sort(result,new Comparator<Record>() {
    					@SuppressWarnings({ "unchecked", "rawtypes" })
    					public int compare(Record r1, Record r2) {
    						int ret = 0;
    						for (int i = 0 ; ret == 0 && i < orderByPassed.length ;  i ++ ){
    							String[] orderByColumnSplit = splitOrderByColumn(orderByPassed[i]);
    							String fieldName = orderByColumnSplit[0];
    							Class<?> fieldType = ref.getFieldGetter(fieldName).getReturnType();
    							TypeConverter<?> converter = Database.getJdbcTypeHelper(getPool()).getTypeRef(fieldType).getTypeConverter();
    							
    							Comparable v1  = (Comparable)(converter.valueOf(r1.get(orderByColumnSplit[0])));
    							Comparable v2  = (Comparable)(converter.valueOf(r2.get(orderByColumnSplit[0])));
    							ret = v1.compareTo(v2);
    							if (ret != 0 && orderByColumnSplit[1].equalsIgnoreCase("DESC")){
    								ret *= -1 ;
    							}
    						}
    						return ret;
    					}
            		});
        			sorting.stop();
        		}
        		
        		Timer processingCache = cat.startTimer("Processing cached records");
        		ret = new ArrayList<M>();
        		for (Iterator<Record> recordIterator  = result.iterator(); 
        				(maxRecords == Select.MAX_RECORDS_ALL_RECORDS || ret.size() < maxRecords ) && recordIterator.hasNext() ; ){
        			Record r = recordIterator.next();
        			M m = r.getAsProxy(modelInterface);
        			if (filter == null || filter.pass(m)){
        				ret.add(m);
        			}
        		}
        		processingCache.stop();
        	}
    	
        	return ret;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (st != null){
                try {
                    st.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

	private Expression whereExpression ;
	public Select where(Expression expression){
		this.whereExpression = expression;
		if (this.whereExpression != null && tableNames != null && tableNames.length == 1 &&
				Config.instance().getBooleanProperty("swf.encryption.support",false)){
			ModelReflector<? extends Model> ref = ModelReflector.instance(Table.modelClass(tableNames[0],getPool()));
			this.whereExpression.encryptBindValuesIfRequired(ref);
		}
		return this;
	}
	
	public Expression getWhereExpression(){
		return whereExpression;
	}

	public static interface ResultFilter<M extends Model> {
		public boolean pass(M record);
	}
	
	public static final class AccessibilityFilter<M extends Model> implements ResultFilter<M> {
		private User user ;
		public AccessibilityFilter(){
			this(Database.getInstance().getCurrentUser());
		}
		public AccessibilityFilter(User user){
			this.user = user;
		}
		public boolean pass(M record) {
			return user == null || record.isAccessibleBy(user);
		}
	}
}
