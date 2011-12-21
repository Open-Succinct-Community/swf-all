package com.venky.swf.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.venky.swf.db.table.BindVariable;


public class Expression {
	String columnName = null;
	List<BindVariable> values = new ArrayList<BindVariable>() ;
	Operator op = null;
	Select selectStmt = null;
	public Expression(String columnName,Operator op, BindVariable... values){
		this.columnName = columnName;
		this.op = op;
		if (values != null && values.length > 0){
			this.values.addAll(Arrays.asList(values));
		}
	}
	
	String conjunction = null;
	public Expression(String conjunction){
		this.conjunction = conjunction;
	}

	public Expression(String columnName,Operator op, Select selectStmt){
		this.columnName = columnName;
		this.op = op;
		this.selectStmt = selectStmt;
		this.values.addAll(selectStmt.getValues());
	}	
	
	private List<Expression> connected = new ArrayList<Expression>();
	public Expression add(Expression expression){
		connected.add(expression);
		values.addAll(expression.getValues());
		return this;
	}

	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		if (conjunction == null){
			builder.append(columnName);
			builder.append(" ");
			if (values == null || values.isEmpty()){
				if (op == Operator.EQ){
					builder.append(" IS NULL ");
				}else {
					builder.append(" IS NOT NULL ");
				}
			}else {
				builder.append(op.toString());
				if (selectStmt != null){
					builder.append(" (");
					builder.append(selectStmt.getParameterizedSQL());
					builder.append(" )");
				}else {
					if (op.requiresParen()){
						builder.append(" ( ");
					}
					 
					for (int i = 0 ; i < values.size() ; i++){
						if (i != 0){
							//To handle In clause.
							builder.append(",");
						}
						builder.append(" ?");
					}
					
					if (op.requiresParen()){
						builder.append(" ) ");
					}
				}
			}
		}else if (!connected.isEmpty()){
			builder.append(" ( ");
			Iterator<Expression> i = connected.iterator();
			while(i.hasNext()){
				Expression expression = i.next();
				builder.append(" ( ");
				builder.append(expression);
				builder.append(" ) ");
				if (i.hasNext()){
					builder.append(conjunction);
				}
			}
			builder.append(" ) ");
		}
		return builder.toString();
	}
	
	public List<BindVariable> getValues(){
		return values;
	}
	
	public boolean isEmpty(){
		return toString().length() == 0;
	}
}
