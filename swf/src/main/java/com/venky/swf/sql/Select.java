package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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

public class Select extends SqlStatement{
	private String[] columnNames = null;
	private String[] tableNames ;
	private String[] orderBy;
	private String[] groupBy;
	protected boolean lock = false;
	public Select(String... columnNames){
		this(false,columnNames);
	}
	public Select(boolean lock,String...columnNames){
		this.lock = lock;
		this.columnNames = columnNames;
	}
	
	public Select from(Class<? extends Model>... models){
		String[] tables = new String[models.length];
		for (int i = 0 ; i< models.length ; i++){
			ModelReflector<? extends Model> ref = ModelReflector.instance(models[i]);
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
			builder.append(" FOR UPDATE ");
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
	public <M extends Model> List<M> execute(Class<M> modelInterface,ResultFilter filter){
		return execute(modelInterface,MAX_RECORDS_ALL_RECORDS,lock,filter);
	}
	public <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords) {
		return execute(modelInterface,maxRecords,lock,null);
	}
	public <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,ResultFilter filter){
		return execute(modelInterface,maxRecords,lock,filter);
	}
	
	
	protected <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords,boolean locked,ResultFilter filter) {
        PreparedStatement st = null;
        try {
        	ModelReflector<M> ref = ModelReflector.instance(modelInterface);
        	QueryCache cache = Database.getInstance().getCache(ref);
        	boolean requireResultSorting = false;
        	Set<Record> result = cache.getCachedResult(getWhereExpression(),maxRecords,locked);
        	if (result == null){
	            Timer queryTimer = Timer.startTimer(getRealSQL());
	            try {
		            st = prepare();
		            if (maxRecords > 0){
		            	st.setMaxRows(maxRecords+1); //Request one more so that you can know if the list is complete or not.
		            }
		            result = new SequenceSet<Record>();
		            if (st.execute()){
		                ResultSet rs = st.getResultSet();
		                while (rs.next()){
		                    Record r = new Record();
		                    r.load(rs);
		                    r.setLocked(locked);
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
		                    result.add(r);
		                }
		                rs.close();
		            }
		            if (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || result.size() <= maxRecords){ // We are requesting maxRecords + 1;!
		            	//We have fetched every thing. Hence cache the whereClause.
		            	cache.setCachedResult(getWhereExpression(), result);
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
	        	}
	        	if (requireResultSorting && orderBy != null){
	        		Collections.sort(ret,new Comparator<M>() {
						public int compare(M o1, M o2) {
							Record r1 = o1.getRawRecord();
							Record r2 = o2.getRawRecord();
							int ret = 0;
							for (int i = 0 ; ret == 0 && i < orderBy.length ;  i ++ ){
								Comparable v1  = (Comparable)r1.get(orderBy[i]);
								Comparable v2  = (Comparable)r2.get(orderBy[i]);
								ret = v1.compareTo(v2);
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
