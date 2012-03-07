package com.venky.swf.sql;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;


public class Delete extends DataManupulationStatement {
	private String table ;
	public Delete (Class<? extends Model> model){
		this(Database.getInstance().getTable(model).getRealTableName());
	}
	private Delete(String table){
		this.table = table;
	}
	
	
	@Override
	public void finalizeParameterizedSQL() {
		StringBuilder builder = getQuery();
		builder.append("DELETE FROM ").append(table);
		Expression where = getWhereExpression();
		if (where != null){
			builder.append(" WHERE ");
			builder.append(where.getParameterizedSQL());
			getValues().addAll(where.getValues());
		}
	}

	private Expression whereExpression ;
	public Delete where(Expression expression){
		this.whereExpression = expression;
		return this;
	}
	
	public Expression getWhereExpression(){
		return whereExpression;
	}

}
