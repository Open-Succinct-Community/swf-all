package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.venky.core.collections.SequenceSet;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.SWFTimeoutException;

public class Select extends SqlStatement{
	
	private static Logger logger = Logger.getLogger(Select.class.getName());
	
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
			/*TODO Removable code. As lock release is now handled
			if (lock && ref.isAnnotationPresent(CONFIGURATION.class)){
				lock = false;
				//Because Config cache stays even after commit and is shared by all threads.
				logger.warning("Select for update downgraded to select for config table " + ref.getTableName());
			}*/
		}
		return from(tables);
	}
	
	private Select from(String... tables){
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
	
	private <M extends Model> boolean allOrderByColumnsAreReal(Class<M> modelClass){
		if (orderBy == null){
			return false;
		}
		ModelReflector<M> ref = ModelReflector.instance(modelClass);
		
		for (int i = 0 ; i < orderBy.length ; i ++ ){
			String[] split = splitOrderByColumn(orderBy[i]);
			String column = split[0];
			if (ref.getColumnDescriptor(ref.getFieldName(column)).isVirtual()){
				return false;
			}
		}
		return true;
	}
	
	protected void finalizeParameterizedSQL(){
		StringBuilder builder = getQuery();
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
        	result = cache.getCachedResult(getWhereExpression(),(orderByPassed != null ? Select.MAX_RECORDS_ALL_RECORDS :maxRecords),locked);
        	
        	if (result == null){
	            Timer queryTimer = Timer.startTimer(getRealSQL());
	            logger.fine(getRealSQL());
	            try {
		            st = prepare();
		            if (this.orderBy != null){
		            	sortResults = false;
		            }
	            	if (!wait && (!lock || (lock && !Database.getJdbcTypeHelper().isNoWaitSupported())) && Database.getJdbcTypeHelper().isQueryTimeoutSupported()){
	            		Logger.getLogger(getClass().getName()).fine("Setting Statement Time out");
	            		st.setQueryTimeout(10);
	            	}
		            result = new SequenceSet<Record>();
		        	ret = new ArrayList<M>();
		            if (st.execute()){
		                ResultSet rs = st.getResultSet();
		                while (rs.next() && (maxRecords == Select.MAX_RECORDS_ALL_RECORDS || ret.size() < maxRecords + 1)){
		                    Record r = new Record();
		                    r.load(rs,ref);
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
	            	if (Database.getJdbcTypeHelper().isQueryTimeoutException(ex)){
	            		throw new SWFTimeoutException(ex);
	            	}else {
	            		throw ex;
	            	}
	            }finally{
	            	queryTimer.stop();
	            }
        	}else {
        		if (sortResults && orderByPassed != null && orderByPassed.length > 0){
            		Collections.sort(result,new Comparator<Record>() {
    					@SuppressWarnings({ "unchecked", "rawtypes" })
    					public int compare(Record r1, Record r2) {
    						int ret = 0;
    						for (int i = 0 ; ret == 0 && i < orderByPassed.length ;  i ++ ){
    							String[] orderByColumnSplit = splitOrderByColumn(orderByPassed[i]);
    							String fieldName = orderByColumnSplit[0];
    							Class<?> fieldType = ref.getFieldGetter(fieldName).getReturnType();
    							TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(fieldType).getTypeConverter();
    							
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
        		}
        		ret = new ArrayList<M>();
        		for (Iterator<Record> recordIterator  = result.iterator(); 
        				(maxRecords == Select.MAX_RECORDS_ALL_RECORDS || ret.size() < maxRecords ) && recordIterator.hasNext() ; ){
        			Record r = recordIterator.next();
        			M m = r.getAsProxy(modelInterface);
        			if (filter == null || filter.pass(m)){
        				ret.add(m);
        			}
        		}
        	}
    	
    		logger.fine("Returning " + ret.size() + " when maxRecords Requested =" + maxRecords );
        	return ret;
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
