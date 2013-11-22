package com.venky.swf.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.StringReader;
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
            if (value.getJdbcType() == Types.BLOB || value.getJdbcType() == Types.LONGVARBINARY || value.getJdbcType() == Types.BINARY){
            	ByteArrayInputStream in = value.getBinaryInputStream();
            	if (in != null){
            		Database.getJdbcTypeHelper().setBinaryStream(st,i+1,in);
            	}else {
            		st.setNull(i+1, value.getJdbcType());
            	}
            }else if (value.getJdbcType() == Types.CLOB || value.getJdbcType() == Types.LONGVARCHAR){
            	StringReader reader = value.getCharacterInputStream();
            	st.setCharacterStream(i+1, reader, reader.length());
            }else {
            	if (value.getValue() == null){
            		st.setNull(i+1, value.getJdbcType());
            	}else {
            		st.setObject(i+1,value.getValue()); //3 parameter  setObject not supported by some drivers.
            	}
            }
        }

        return st;
    }

	private String realSQL = null;
	public String getRealSQL(){
		if (realSQL != null) {
			return realSQL;
		}
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
		
		String sql = builder.toString();
		if (finalized){
			realSQL = sql;
		}
		return sql;
	}

	private StringBuilder query = new StringBuilder();
	private List<BindVariable> values = new ArrayList<BindVariable>();
	private boolean finalized = false;
	private String getParameterizedSQL() {
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
