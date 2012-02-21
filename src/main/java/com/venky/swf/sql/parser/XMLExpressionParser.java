package com.venky.swf.sql.parser;

import java.io.InputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.xml.XMLDocument;
import com.venky.xml.XMLElement;

public class XMLExpressionParser {
	Table<? extends Model> table = null;
	public XMLExpressionParser(Class<? extends Model> modelClass){
		table = Database.getInstance().getTable(modelClass);
	}
	public Expression parse(InputStream is) {
		return parse(StringUtil.read(is));
	}
	public Expression parse(String input){
		XMLDocument docExpression = XMLDocument.getDocumentFor(input);
		return parse(docExpression.getDocumentRoot());
	}
	
	public Expression parse(XMLElement elem) {
		Expression e = null;
		if (elem.getNodeName().equals(Conjunction.AND.toString()) || elem.getNodeName().equals(Conjunction.OR.toString())){
			if (elem.getNodeName().equals(Conjunction.AND.toString())){
				e = new Expression(Conjunction.AND);
			}else {
				e = new Expression(Conjunction.OR);
			}
			for (Iterator<XMLElement> childIter = elem.getChildElements() ; childIter.hasNext() ;){
				XMLElement child = childIter.next();
				e.add(parse(child));
			}
		}else {
			XMLElement columnName = elem.getChildElement("ColumnName");
			int columnType = table.getColumnDescriptor(columnName.getNodeValue()).getJDBCType();
			
			List<BindVariable> bvalues =  new ArrayList<BindVariable>();
			Operator op = getOperator(elem.getNodeName());
			
			if (op.isMultiValued()){
				XMLElement values = elem.getChildElement("Values");
				for (Iterator<XMLElement> valueIter  = values.getChildElements() ; valueIter.hasNext() ; ){
					addBindVariable(bvalues, columnType, valueIter.next());
				}
			}else{
				XMLElement eValue = elem.getChildElement("Value");
				addBindVariable(bvalues, columnType, eValue);
			}
			e = new Expression(columnName.getNodeValue(),op,bvalues.toArray(new BindVariable[]{}));
		}
			
		return e;
	}
	public Operator getOperator(String s){
		if (s.equals("EQ")){
			return Operator.EQ;
		}else if (s.equals("GE")){
			return Operator.GE;
		}else if (s.equals("GT")){
			return Operator.GT;
		}else if (s.equals("IN")){
			return Operator.IN;
		}else if (s.equals("LE")){
			return Operator.LE;
		}else if (s.equals("LK")){
			return Operator.LK;
		}else if (s.equals("LT")){
			return Operator.LT;
		}else if (s.equals("NE")){
			return Operator.NE;
		}else {
			throw new UnsupportedOperationException(s);
		}
	}
	
	private void addBindVariable(List<BindVariable> bValues,int columnType , XMLElement eValue) {
		String sValue = eValue.getNodeValue();
		
		Object value = sValue; 
		if (columnType != Types.VARCHAR){
			value = Database.getInstance().getJdbcTypeHelper().getTypeRef(columnType).getTypeConverter().valueOf(sValue);
		}
		bValues.add(new BindVariable(value,columnType));

	}
}
