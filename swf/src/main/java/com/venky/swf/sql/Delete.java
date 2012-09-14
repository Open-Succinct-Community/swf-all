package com.venky.swf.sql;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;



public class Delete extends DataManupulationStatement {
	private String table ;
	public <M extends Model> Delete(ModelReflector<M> ref){
		this(ref.getTableName());
	}
	
	public Delete(String table){
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
