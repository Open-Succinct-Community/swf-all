/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Types;

import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;

/**
 *
 * @author venky
 */
public class BindVariable {
    private final TypeRef<?> ref;
    private final Object value;
    public BindVariable(Object value){
        this(value,Database.getInstance().getJdbcTypeHelper().getTypeRef(value.getClass()));
    }
    public BindVariable(Object value,int jdbcType){
    	this(value,Database.getInstance().getJdbcTypeHelper().getTypeRef(jdbcType));
    }
    public BindVariable(Object value, TypeRef<?> ref){
    	this.ref = ref;
    	this.value = value;
    }
    

    public int getJdbcType() {
        return ref.getJdbcType();
    }

    public Object getValue() {
        return value;
    }
    
    public Reader getCharacterInputStream(){ 
    	int jdbcType = getJdbcType();
    	if ( jdbcType != Types.LONGVARCHAR && jdbcType != Types.CLOB ){
    		return null;
    	}
    	return (Reader)ref.getTypeConverter().valueOf(value);
    }

    public InputStream getBinaryInputStream(){ 
    	int jdbcType = getJdbcType();
    	if ( jdbcType != Types.LONGVARBINARY && jdbcType != Types.BLOB ){
    		return null;
    	}
    	return (InputStream)ref.getTypeConverter().valueOf(value);
    }

}
