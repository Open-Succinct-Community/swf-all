package com.venky.swf.sql;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.venky.swf.db.table.BindVariable;

public class Insert extends DataManupulationStatement{
	private String table = null;
	private Map<String,BindVariable> values = null;
	private SortedSet<String> keys = new TreeSet<String>();
	public Insert(String table){
		this.table = table;
	}
	
	public Insert values(Map<String,BindVariable> values){
		this.values = values;
		keys.addAll(values.keySet());
		return this;
	}

	@Override
	protected void finalizeParameterizedSQL() {
		StringBuilder q = getQuery();

		q.append("INSERT into ").append(table); 
		q.append(" ( ");
		Iterator<String> ki = keys.iterator();
		
		StringBuilder valueList = new StringBuilder();
		while (ki.hasNext()){
			String key = ki.next();
			q.append(key);
			valueList.append("?");
			getValues().add(values.get(key));
			if (ki.hasNext()){
				q.append(" , ");
				valueList.append(" , ");
			}
		}
		q.append(" ) values (");
		q.append(valueList.toString());
		q.append(" )");
	}

}
