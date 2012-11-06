package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.SWFTimeoutException;

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
	
	@SuppressWarnings("unchecked")
	public Select from(Class<?>... models){
		String[] tables = new String[models.length];
		for (int i = 0 ; i< models.length ; i++){
			if (!Model.class.isAssignableFrom(models[i])){
				continue;
			}
			ModelReflector<? extends Model> ref = ModelReflector.instance((Class<? extends Model>)models[i]);
			tables[i] = ref.getTableName();
			if (lock && ref.isAnnotationPresent(CONFIGURATION.class)){
				lock = false;
				//Because Config cache stays even after commit and is shared by all threads.
				Logger.getLogger(getClass().getName()).warning("Select for update downgraded to select for config table " + ref.getTableName());
			}
		}
		return from(tables);
	}
	
	private Select from(String... tables){
		this.tableNames = tables;
		return this;
	}
	
	
	public Select orderBy(String... columnNames){
		this.orderBy = columnNames;
		return this;
	}
	
	public Select groupBy(String... columnNames){
		this.groupBy = columnNames;
		return this;
	}
	
	protected void finalizeParameterizedSQL(){
		StringBuilder builder = getQuery();
		builder.append("SELECT ");
		if (columnNames == null || columnNames.length == 0){
			builder.append(" * ");
		}else {
			addlist(builder, columnNames);
		}
		builder.append(" FROM ");
		addlist(builder, tableNames);
		builder.append(" ");
		
		Expression where = getWhereExpression();
		if (where != null && !where.isEmpty()){
			builder.append(" WHERE ");
			builder.append(where.getParameterizedSQL());
			getValues().addAll(where.getValues());
		}
		
		if (groupBy != null){
			builder.append(" GROUP BY ");
			addlist(builder, groupBy);
		}
		
		if (orderBy != null){
			builder.append(" ORDER BY ");
			addlist(builder, orderBy);
		}
		
		if (lock){
			builder.append(Database.getJdbcTypeHelper().getForUpdateLiteral());

			if (!wait && Database.getJdbcTypeHelper().isNoWaitSupported()){
				builder.append(Database.getJdbcTypeHelper().getNoWaitLiteral());
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
			builder.append(strings[i]);
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
	
	private boolean isCacheable(ModelReflector<? extends Model> ref){
		return (columnNames == null || columnNames.length == 0) && (ref.getRealModelClass() != null) && (orderBy == null || orderBy.length == 0);
	}
	
	private String[] splitOrderByColumn(int i){
		StringTokenizer tok = new StringTokenizer(orderBy[i]);
		String columnName = tok.nextToken();
		String orderByType = "ASC";
		if (tok.hasMoreTokens()){
			orderByType =  tok.nextToken();
		}
		return new String[]{columnName,orderByType};
	}
	
	protected <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,boolean locked,ResultFilter<M> filter) {
        PreparedStatement st = null;
        try {
        	ModelReflector<M> ref = ModelReflector.instance(modelInterface);
        	Set<Record> result = null;
        	QueryCache cache = null;
        	if (isCacheable(ref)){
	        	cache = Database.getInstance().getCache(ref);
	        	result = cache.getCachedResult(getWhereExpression(),maxRecords,locked);
        	}
        	boolean requireResultSorting = false;
        	if (result == null){
	            Timer queryTimer = Timer.startTimer(getRealSQL());
	            try {
		            st = prepare();
		            if (maxRecords > 0){
		            	st.setMaxRows(maxRecords+1); //Request one more so that you can know if the list is complete or not.
		            }
	            	if (!wait && (!lock || (lock && !Database.getJdbcTypeHelper().isNoWaitSupported())) && Database.getJdbcTypeHelper().isQueryTimeoutSupported()){
	            		Logger.getLogger(getClass().getName()).fine("Setting Statement Time out");
	            		st.setQueryTimeout(10);
	            	}
		            result = new SequenceSet<Record>();
		            if (st.execute()){
		                ResultSet rs = st.getResultSet();
		                while (rs.next()){
		                    Record r = new Record();
		                    r.load(rs,ref);
		                    r.setLocked(locked);
		                    if (cache != null){
			                    Record cachedRecord = cache.getCachedRecord(r);
			                    if (cachedRecord != null ){
			                    	if (!locked || locked == cachedRecord.isLocked()){
			                    		r = cachedRecord;
			                    	}else {
			                    		cache.registerUpdate(r);
			                    	}
			                    }else {
			                    	cache.add(r);
			                    }
		                    }
		                    result.add(r);
		                }
		                rs.close();
		            }
		            if (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || result.size() <= maxRecords){ // We are requesting maxRecords + 1;!
		            	//We have fetched every thing. Hence cache the whereClause.
		            	if (cache != null){
		            		cache.setCachedResult(getWhereExpression(), result);
		            	}
		            }
	            }catch (SQLException ex){
	            	if (Database.getJdbcTypeHelper().isQueryTimeoutException(ex)){
	            		throw new SWFTimeoutException(ex);
	            	}else {
	            		throw ex;
	            	}
	            }finally{
	            	queryTimer.stop();
	            }
        	}else {
            	requireResultSorting = true;
        	}
        	Timer creatingProxies = Timer.startTimer("creatingProxies");
        	try {
	        	List<M> ret = new ArrayList<M>();
	        	for (Record record: result){
	                M m = record.getAsProxy(modelInterface);
	                if (filter == null || filter.pass(m)){
	                	ret.add(m);
	                }
	                if (maxRecords > 0 && ret.size() >= maxRecords){
	                	break;
	                }
	        	}
	        	if (requireResultSorting && orderBy != null && orderBy.length > 0){
	        		Collections.sort(ret,new Comparator<M>() {
						@SuppressWarnings({ "unchecked", "rawtypes" })
						public int compare(M o1, M o2) {
							Record r1 = o1.getRawRecord();
							Record r2 = o2.getRawRecord();
							int ret = 0;
							for (int i = 0 ; ret == 0 && i < orderBy.length ;  i ++ ){
								String[] orderByColumnSplit = splitOrderByColumn(i);
								
								Comparable v1  = (Comparable)r1.get(orderByColumnSplit[0]);
								Comparable v2  = (Comparable)r2.get(orderByColumnSplit[0]);
								ret = v1.compareTo(v2);
								if (ret != 0 && orderByColumnSplit[1].equalsIgnoreCase("DESC")){
									ret *= -1 ;
								}
							}
							return ret;
						}
					});
	        	}
	        	return ret;
        	}finally {
        		creatingProxies.stop();
        	}
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (st != null){
                try {
                    if (!st.isClosed()){
                        st.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

	private Expression whereExpression ;
	public Select where(Expression expression){
		this.whereExpression = expression;
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
