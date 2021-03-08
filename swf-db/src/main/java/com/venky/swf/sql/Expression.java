package com.venky.swf.sql;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.ModelInvocationHandler;
import com.venky.swf.db.table.Record;

import javax.xml.crypto.Data;


public class Expression {
	String columnName = null;
	List<BindVariable> values = null ;
	Operator op = null;
	public static final int CHUNK_SIZE = 30;


	public static Expression createExpression(String pool,String columnName, Operator op, Object... values){
		List<List<Object>> chunks = getValueChunks(Arrays.asList(values));
		Expression e = new Expression(pool,Conjunction.OR);
		for (List<Object> chunk : chunks){
			e.add(new Expression(pool,columnName,op,chunk.toArray()));
		}
		return e;
	}
	
	public static List<List<Object>> getValueChunks(List<Object> values){
		List<List<Object>> chunks = new ArrayList<List<Object>>();
		chunks.add(new ArrayList<Object>());

		for (Object bv: values){
			List<Object> aChunk = chunks.get(chunks.size()-1);
			
			if (aChunk.size() >= CHUNK_SIZE){
				aChunk = new ArrayList<Object>();
				chunks.add(aChunk);
			}
			
			if (bv != null){
				aChunk.add(bv);
			}else {
				if (!aChunk.isEmpty()){
					chunks.add(new ArrayList<Object>());
				}
				chunks.add(new ArrayList<Object>());
			}
		}
		return chunks;
	}

	String pool = null;
	String realColumnName = null;
	String functionName = null;
	public boolean isLowerFunction(){
		return functionName != null && Database.getJdbcTypeHelper(pool).getLowerCaseFunction().equalsIgnoreCase(functionName);
	}
	@SafeVarargs
	public <T> Expression(String pool,String columnName,Operator op, T... values){
		this.columnName = columnName;
		if (columnName.contains("(")){
			this.realColumnName = columnName.substring(columnName.indexOf("(")+1,columnName.lastIndexOf(")"));
			this.functionName = columnName.substring(0,columnName.indexOf("("));
		}else {
			this.realColumnName = columnName;
		}

		this.op = op ;
		this.values = new SequenceSet<BindVariable>();
		this.pool = pool;
		
		try {
			for (int i = 0 ; i < values.length ; i ++ ){
				if (values[i] instanceof BindVariable) {
					this.values.add((BindVariable)values[i]);	
				}else {
					this.values.add(new BindVariable(pool,values[i]));
				}
			}
		}catch (NullPointerException ex){
			MultiException mex =  new MultiException("NPE found while creating expression for " + columnName + op.toString()  );
			mex.add(ex);
			throw mex;
		}
		
		setFinalized(true);
	}
	Conjunction conjunction = null;
	public Expression(String pool,Conjunction conjunction){
		this.pool = pool;
		this.conjunction = conjunction;
		this.values = new ArrayList<BindVariable>();
	}

	private boolean finalized = false;
	private boolean isFinalized(){
		return finalized;
	}
	
	private void setFinalized(boolean finalized){
		this.finalized = finalized;
	}
	
	private void ensureModifiable(){
		if (isFinalized()){
			throw new ExpressionFinalizedException();
		}
	}
	
	public static class ExpressionFinalizedException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1865905730160016333L;
		
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
		ensureModifiable();
		expression.setParent(this);
		connected.add(expression);
		addValues(expression.getValues());
		return this;
	}
	
	private void addValues(List<BindVariable> values){
		ensureModifiable();
		this.values.addAll(values);
		if (parent != null){
			parent.addValues(values);
		}
	}
	
	private String realSQL = null;
	public String getRealSQL(){
		if (realSQL != null){
			return realSQL;
		}
		StringBuilder builder = new StringBuilder(getParameterizedSQL());
		List<BindVariable> parameters = getValues();
		
		int index = builder.indexOf("?");
		int p = 0;
		while (index >= 0) {
			BindVariable parameter = parameters.get(p);
			String pStr = StringUtil.valueOf(parameter.getValue()) ;
			if (Database.getJdbcTypeHelper(pool).getTypeRef(parameter.getJdbcType()).isQuotedWhenUnbounded()){
				pStr = "'" + pStr + "'";
			}
			builder.replace(index, index+1, pStr);
			p+=1;
			index = builder.indexOf("?",index+pStr.length());
		}
		
		String sql = builder.toString();
		if (isFinalized()){
			realSQL = sql;
		}
		return sql;
	}
	
	private String parameterizedSQL = null ;
	public String getParameterizedSQL(){
		if (parameterizedSQL != null){
			return parameterizedSQL;
		}
		
		StringBuilder builder = new StringBuilder();
		if (conjunction == null){
			builder.append(columnName);
			builder.append(" ");
			if (values == null || values.isEmpty()){
				if (op == Operator.EQ || op == Operator.IN){
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
					builder.append(" ? ");
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
		
		String sql = builder.toString();
		if (isFinalized()){
			parameterizedSQL = sql;
		}
		return sql;
	}
	
	public List<BindVariable> getValues(){
		return Collections.unmodifiableList(values);
	}
	
	public boolean isEmpty(){
		boolean empty = false;
		if (conjunction != null){
			empty = true; 
			for (Iterator<Expression> i = connected.iterator();i.hasNext() && empty ; ){
				empty = i.next().isEmpty();
			}
		}
		
		return empty;
	}
	
	private Integer hashValue = null;
	@Override
	public int hashCode(){
		if (hashValue == null){
			setFinalized(true);
			hashValue = getRealSQL().hashCode();
		}
		return hashValue;
	}
	
	public boolean equals(Object other){
		if (other == null){
			return false;
		}
		if (!(other instanceof Expression)){
			return false;
		}
		Expression e = (Expression) other;
		setFinalized(true);
		e.setFinalized(true);
		if (hashCode() != e.hashCode()){
			return false;
		}else {
			return getRealSQL().equals(e.getRealSQL());
		}
	}
	
	private Object get(Object record, String columnName){
		boolean isModelProxyObject = Proxy.isProxyClass(record.getClass()) && (record instanceof Model);
		Object value = null;
		String constantIdentifyingRegExp = "^('[^']*')|([0-9]*)$" ;

		if (columnName.matches(constantIdentifyingRegExp)){
			return columnName; //A Constant
		}
		if (isModelProxyObject){
			Model m = (Model)record;
			ModelInvocationHandler h = (ModelInvocationHandler) Proxy.getInvocationHandler(m);
			String fieldName = columnName ; 
			if (!h.getReflector().getFields().contains(fieldName)) {
				fieldName = h.getReflector().getFieldName(columnName);
				if (fieldName == null){
					throw new IllegalArgumentException(columnName + " is neither a column nor field" ); 
				}
			}
			value = h.getReflector().get(m,fieldName);
		}else if (Record.class.isInstance(record)){
			value = ((Record)record).get(columnName);
		}else {
			throw new RuntimeException("Don't know how to get column value from object of type " + record.getClass() + " for column " + columnName);
		}
		return value;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public  boolean eval(Object record){
		if (conjunction == null){
			Object value = get(record,getColumnName());
			if (value != null && value instanceof  String && isLowerFunction()){
				value = ((String) value).toLowerCase();
			}
			if (value == null){
				if (values.isEmpty() || values.contains(null)){
					return (op == Operator.EQ || op == Operator.IN);
				}else {
					return op == Operator.NE;
				}
			}else if (values.isEmpty()){
				if (op == Operator.EQ || op == Operator.IN){
					return false;
				}else if (op == Operator.NE){
					return true;
				}
			}
			//value not null
			if (values.size() == 1){
				Object v = values.get(0).getValue();
				if (v == null){
					return false;
				}
				if (v.getClass() != value.getClass()){
					value = Database.getJdbcTypeHelper(pool).getTypeRef(v.getClass()).getTypeConverter().valueOf(value);
					//Compare Apples and apples not apples and oranges.
				}
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
				if (values.contains(new BindVariable(pool,value))){
					return true;
				}
			}
		}else if (conjunction == Conjunction.OR){
			boolean ret = connected.isEmpty();
			for (Iterator<Expression> i = connected.iterator(); !ret && i.hasNext() ;){
				Expression e = i.next();
				ret = ret || e.eval(record);
			}
			return ret;
		}else if (conjunction == Conjunction.AND){
			boolean ret = true;
			for (Iterator<Expression> i = connected.iterator(); ret && i.hasNext() ;){
				Expression e = i.next();
				ret = ret && e.eval(record);
			}
			return ret;
		}
		return false;
	}
	public <M extends Model> void encryptBindValuesIfRequired(ModelReflector<M> reflector){
		if (reflector.getEncryptedFields().isEmpty()){
			return;
		}
		if (conjunction == null){
			String fieldName = reflector.getFieldName(columnName);
			if (!ObjectUtil.isVoid(fieldName) && reflector.isFieldEncrypted(fieldName)){
				if (values != null && !values.isEmpty()){
					for (int i = 0 ; i < values.size() ; i++){
						values.get(i).encrypt();
					}
				}
			}
		}else if (!connected.isEmpty()){
			Iterator<Expression> i = connected.iterator();
			while(i.hasNext()){
				Expression expression = i.next();
				expression.encryptBindValuesIfRequired(reflector);
			}
		}
	}


	public <M extends Model> String toLucene(Class<M> modelClass) {
		StringBuilder builder = new StringBuilder();
		if (conjunction == null){
			if (ModelReflector.instance(modelClass).getIndexedColumns().contains(columnName) || "ID".equals(columnName)){
				if (values == null || values.isEmpty()){
					if (op == Operator.EQ || op == Operator.IN){
						builder.append(" ( ");
						builder.append(columnName);
						builder.append(":NULL ");
						builder.append(" ) ");
					}/*else {
						builder.append("NOT ");
						builder.append(columnName);
						builder.append(":NULL ");
					}*/
				}else {
					builder.append(" ( ");
					for (int i = 0 ; i < values.size() ; i++){
						if (i != 0){
							builder.append(" OR ");
						}
						builder.append(columnName);
						builder.append(":");
						builder.append(values.get(i).getValue());
					}
					builder.append(" ) ");
				}
			}
		}else if (!connected.isEmpty()){
			int numExpressionsConnected = 0 ;  
			
			Iterator<Expression> i = connected.iterator();
			while(i.hasNext()){
				Expression expression = i.next();
				String luceneString = expression.toLucene(modelClass);
				if (!ObjectUtil.isVoid(luceneString)){
					if (builder.length() > 0){
						builder.append(" ");
						builder.append(conjunction);
						builder.append(" ");
					}
					builder.append(luceneString);
					numExpressionsConnected ++;
				}
			}
			if (builder.length() > 0 && numExpressionsConnected > 1){ //avoid frivolous brackets
				builder.insert(0,"( ");
				builder.append(" )");
			}
		}
		
		return builder.toString();
	}
	public String getColumnName(){
		return realColumnName;
    }
    public Operator getOperator(){
	    return op;
    }

    public Conjunction getConjunction() {
        return conjunction;
    }
}
