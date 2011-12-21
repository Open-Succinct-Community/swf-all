package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.BindVariable;

public class SqlStatement {
	
	protected PreparedStatement prepare(String... columnNames) throws SQLException{
        PreparedStatement st = null;
        if (columnNames == null || columnNames.length == 0){
            st = Database.getInstance().getCurrentTransaction().createStatement(getParameterizedSQL());
        }else {
            st = Database.getInstance().getCurrentTransaction().createStatement(getParameterizedSQL(),columnNames);
        }
        
        List<BindVariable> parameters = getValues();
        for (int i = 0; i < parameters.size() ; i ++ ) {
            BindVariable value = parameters.get(i);
            if (value.getJdbcType() == Types.BLOB || value.getJdbcType() == Types.LONGVARBINARY){
            	st.setBinaryStream(i+1, value.getBinaryInputStream());
            }else if (value.getJdbcType() == Types.CLOB || value.getJdbcType() == Types.LONGVARCHAR){
            	st.setCharacterStream(i+1, value.getCharacterInputStream());
            }else {
            	st.setObject(i+1,value.getValue(), value.getJdbcType());
            }
        }

        return st;
    }

	public String getNonParameterizedSQL(){
		StringBuilder builder = new StringBuilder(getParameterizedSQL());
		List<BindVariable> parameters = getValues();
		
		int index = builder.indexOf("?");
		int p = 0;
		while (index >= 0) {
			String pStr = StringUtil.valueOf(parameters.get(p).getValue());
			builder.replace(index, index+1, pStr);
			p+=1;
			index = builder.indexOf("?",index+pStr.length());
		}
		
		return builder.toString();
		
	}

	private StringBuilder query = new StringBuilder();
	private List<BindVariable> values = new ArrayList<BindVariable>();
	private boolean finalized = false;
	public String getParameterizedSQL() {
		if (!finalized){
			finalizeParameterizedSQL();
			finalized = true;
		}
		return query.toString();
	}
	
	public StringBuilder getQuery(){
		return query;
	}
	
	public List<BindVariable> getValues() {
		return values;
	}

	public SqlStatement add(String sqlFragment){
		query.append(sqlFragment);
		return this;
	}
	protected void finalizeParameterizedSQL(){
		
	}
	
}
