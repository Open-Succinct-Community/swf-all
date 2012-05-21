package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Record;
import com.venky.swf.db.table.Table;

public class Select extends SqlStatement{
	private String[] columnNames = null;
	private String[] tableNames ;
	private String[] orderBy;
	private String[] groupBy;
	public Select(String... columnNames){
		this.columnNames = columnNames;
	}
	
	public Select from(Class<? extends Model>... models){
		String[] tables = new String[models.length];
		for (int i = 0 ; i< models.length ; i++){
			tables[i] = Database.getTable(models[i]).getRealTableName();
		}
		return from(tables);
	}
	public Select from(String... tables){
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
		for (int i = 0 ; i< tableNames.length ;i ++){
			if (i != 0){
				builder.append(", ");
			}
			builder.append(tableNames[i]);
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
		
	}
	private void addlist(StringBuilder builder,String...strings ){
		if (strings == null){
			return ;
		}
		for (int i = 0; i < strings.length ; i++){
			if (i != 0){
				builder.append(",");
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
		return execute(modelInterface,MAX_RECORDS_ALL_RECORDS);
	}
	
	public <M extends Model> List<M> execute(Class<M> modelInterface,int maxRecords) {
        PreparedStatement st = null;
        try {
        	QueryCache cache = Database.getInstance().getCache(ModelReflector.instance(modelInterface));
        	SortedSet<Record> result = cache.getCachedResult(getWhereExpression(),maxRecords);
        	if (result == null){
	            Timer queryTimer = Timer.startTimer(getRealSQL());
	            try {
		            st = prepare();
		            if (maxRecords > 0){
		            	st.setMaxRows(maxRecords+1); //Rquest one more so that you can know if the list is complete or not.
		            }
		            result = new TreeSet<Record>();
		            if (st.execute()){
		                ResultSet rs = st.getResultSet();
		                while (rs.next()){
		                    Record r = new Record();
		                    r.load(rs);
		                    Record cachedRecord = cache.getCachedRecord(r);
		                    if (cachedRecord != null){
		                    	r = cachedRecord;
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
        	}
        	Timer creatingProxies = Timer.startTimer("creatingProxies");
        	try {
	        	List<M> ret = new ArrayList<M>();
	        	for (Record record: result){
	                M m = record.getAsProxy(modelInterface);
	                ret.add(m);
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

}
