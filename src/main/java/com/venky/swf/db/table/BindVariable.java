/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;

/**
 *
 * @author venky
 */
public class BindVariable {
    private final Object value ; 
    private final int jdbcType ; 
    public BindVariable(Object value){
        this(value,Database.getInstance().getJdbcTypeHelper().getTypeRef(value.getClass()).getJdbcType());
    }
    public BindVariable(Object value,int jdbcType){
        this.value = value; 
        this.jdbcType = jdbcType;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public Object getValue() {
        return value;
    }
    
    public Reader getCharacterInputStream(){ 
    	if ( jdbcType != Types.LONGVARCHAR && jdbcType != Types.CLOB ){
    		return null;
    	}
    	
    	if (value != null){
	    	if (value instanceof Clob){ 
	    		try {
					return ((Clob)value).getCharacterStream();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
	    	}
	    	
	    	if (value instanceof Reader){
	    		return (Reader)value;
	    	}
    	}
    	
		return new StringReader(StringUtil.valueOf(value));
    	
    }

    public InputStream getBinaryInputStream(){ 
    	if ( jdbcType != Types.LONGVARBINARY && jdbcType != Types.BLOB ){
    		return null;
    	}
    	
    	if (value != null){
	    	if (value instanceof Blob){ 
	    		try {
					return ((Blob)value).getBinaryStream();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
	    	}
	    	
	    	if (value instanceof InputStream){
	    		return (InputStream)value;
	    	}
    	}
    	
    	return new ByteArrayInputStream(StringUtil.valueOf(value).getBytes());
    }

}
