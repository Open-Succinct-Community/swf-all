package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;
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
			tables[i] = Database.getInstance().getTable(models[i]).getRealTableName();
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
	
	public <M extends Model> List<M> execute(){
		if (tableNames.length != 1){
			throw new UnsupportedOperationException("Query is a join.Don't know what Collection to return.");
		}
		Table<M> table = Database.getInstance().getTable(tableNames[0]);
		return execute(table.getModelClass());
	}
	
	public <M extends Model> List<M> execute(Class<M> modelInterface) {
        PreparedStatement st = null;
        String query = getRealSQL();
        try {
        	QueryCache<M> cache = Database.getInstance().getCache(modelInterface);
        	List<M> result = cache.getCachedResult(getWhereExpression());
        	if (result != null){
        		return result;
        	}

            Logger.getLogger(getClass().getName()).log(Level.INFO, "Executing {0}", query);
            st = prepare();
            result = new ArrayList<M>();
            if (st.execute()){
                ResultSet rs = st.getResultSet();
                while (rs.next()){
                    Record r = new Record();
                    r.load(rs);
                    M m = ModelImpl.getProxy(modelInterface, r);
                    result.add(m);
                }
                rs.close();
            }
            cache.setCachedResult(getWhereExpression(), result);
            return result;
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
