package com.venky.swf.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Record;


public class Expression {
	String columnName = null;
	List<BindVariable> values = new ArrayList<BindVariable>() ;
	Operator op = null;
	public Expression(String columnName,Operator op, BindVariable... values){
		this.columnName = columnName;
		this.op = op;
		if (values != null && values.length > 0){
			this.values.addAll(Arrays.asList(values));
		}
	}
	
	Conjunction conjunction = null;
	public Expression(Conjunction conjunction){
		this.conjunction = conjunction;
	}

	private List<Expression> connected = new ArrayList<Expression>();
	private Expression parent = null;
	
	public int getNumChildExpressions(){
		return connected.size();
	}
	
	
	public Expression getParent() {
		return parent;
	}

	public void setParent(Expression parent) {
		this.parent = parent;
	}

	public Expression add(Expression expression){
		expression.setParent(this);
		connected.add(expression);
		addValues(expression.getValues());
		return this;
	}
	
	private void addValues(List<BindVariable> values){
		this.values.addAll(values);
		if (parent != null){
			parent.addValues(values);
		}
	}
	public String getRealSQL(){
		StringBuilder builder = new StringBuilder(getParameterizedSQL());
		List<BindVariable> parameters = getValues();
		
		int index = builder.indexOf("?");
		int p = 0;
		while (index >= 0) {
			BindVariable parameter = parameters.get(p);
			String pStr = StringUtil.valueOf(parameter.getValue()) ;
			if (Database.getInstance().getJdbcTypeHelper().getTypeRef(parameter.getJdbcType()).isQuotedWhenUnbounded()){
				pStr = "'" + pStr + "'";
			}
			builder.replace(index, index+1, pStr);
			p+=1;
			index = builder.indexOf("?",index+pStr.length());
		}
		
		return builder.toString();
		
	}
	
	public String getParameterizedSQL(){
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
				if (op.isMultiValued()){
					builder.append(" ( ");
				}
				 
				for (int i = 0 ; i < values.size() ; i++){
					if (i != 0){
						//To handle In clause.
						builder.append(",");
					}
					builder.append(" ?");
				}
				
				if (op.isMultiValued()){
					builder.append(" ) ");
				}
			}
		}else if (!connected.isEmpty()){
			boolean multipleExpressionsConnected = connected.size() > 1; 
			
			Iterator<Expression> i = connected.iterator();
			while(i.hasNext()){
				Expression expression = i.next();
				if (!expression.isEmpty()){
					if (builder.length() > 0){
						builder.append(" ");
						builder.append(conjunction);
						builder.append(" ");
					}
					builder.append(expression.getParameterizedSQL());
				}
			}
			if (builder.length() > 0 && multipleExpressionsConnected){ //avoid frivolous brackets
				builder.insert(0,"( ");
				builder.append(" )");
			}
		}
		return builder.toString();
	}
	
	public List<BindVariable> getValues(){
		return values;
	}
	
	public boolean isEmpty(){
		return conjunction != null && connected.isEmpty() ;
		//return getParameterizedSQL().length() == 0;
	}
	
	@Override
	public int hashCode(){
		return getRealSQL().hashCode();
	}
	
	public boolean equals(Object other){
		if (other == null){
			return false;
		}
		if (!(other instanceof Expression)){
			return false;
		}
		Expression e = (Expression) other;
		return getRealSQL().equals(e.getRealSQL());
	}
	
	public <M extends Model> boolean eval(M m){
		Record record = m.getRawRecord();
		if (conjunction == null){
			Object value = record.get(columnName);
			if (value == null){
				return values.isEmpty();
			}else if (values.isEmpty()){
				return false;
			}
			if (values.size() == 1){
				Object v = values.get(0).getValue();
				if (op == Operator.EQ){
					return ObjectUtil.equals(v,value);
				}else if (value instanceof Comparable){
					if (op == Operator.GE){
						return ((Comparable)value).compareTo(v) >= 0;
					}else if (op == Operator.GT){
						return ((Comparable)value).compareTo(v) > 0;
					}else if (op == Operator.LE){
						return ((Comparable)value).compareTo(v) <= 0;
					}else if (op == Operator.LT){
						return ((Comparable)value).compareTo(v) < 0;
					}else if (op == Operator.NE){
						return ((Comparable)value).compareTo(v) != 0;
					}
				}
				if (op == Operator.LK && value instanceof String && v instanceof String){
					return ((String)value).matches(((String)v).replace("%", ".*"));
				}
			}
			if (op == Operator.IN){
				for (BindVariable v :values){
					if (ObjectUtil.equals(value,v.getValue())){
						return true;
					}
				}
			}
		}else if (conjunction == Conjunction.OR){
			boolean ret = connected.isEmpty();
			for (Iterator<Expression> i = connected.iterator(); !ret && i.hasNext() ;){
				Expression e = i.next();
				ret = ret || e.eval(m);
			}
			return ret;
		}else if (conjunction == Conjunction.AND){
			boolean ret = true;
			for (Iterator<Expression> i = connected.iterator(); ret && i.hasNext() ;){
				Expression e = i.next();
				ret = ret && e.eval(m);
			}
			return ret;
		}
		return false;
	}
	
}
