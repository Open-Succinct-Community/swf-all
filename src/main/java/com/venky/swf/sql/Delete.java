package com.venky.swf.sql;


public class Delete extends DataManupulationStatement {
	private String table ;
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
