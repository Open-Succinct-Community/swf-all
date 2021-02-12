/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.sql.Types;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.util.SharedKeys;

/**
 *
 * @author venky
 */
public class BindVariable {
    private final TypeRef<?> ref;
    private Object value;
    public BindVariable(String pool, Object value){
        this(pool,value,Database.getJdbcTypeHelper(pool).getTypeRef(value.getClass()));
    }
    public BindVariable(String pool,Object value,int jdbcType){
    	this(pool,value,Database.getJdbcTypeHelper(pool).getTypeRef(jdbcType));
    }
    
    public BindVariable(String pool,Object value, TypeRef<?> ref){
    	this.ref = ref;
    	if (ref.getJdbcType() == Types.VARCHAR && value != null && !value.getClass().equals(String.class)){
    		this.value = ref.getTypeConverter().toString(value); //PGSql stores Clobs as Varchar(
    	}else if (ref.getJdbcType() == Database.getJdbcTypeHelper(pool).getTypeRef(Double.class).getJdbcType() && value != null && !(value.getClass().equals(Double.class))){
    		this.value = Database.getJdbcTypeHelper(pool).getTypeRef(Double.class).getTypeConverter().valueOf(value);
    	}else if (ref.getJdbcType() == Database.getJdbcTypeHelper(pool).getTypeRef(Long.class).getJdbcType() && value != null && !(value.getClass().equals(Long.class))){
    		this.value = Database.getJdbcTypeHelper(pool).getTypeRef(Long.class).getTypeConverter().valueOf(value);
    	}else {
    		this.value = value;
    	}
    }
    
    public boolean equals(Object o){
    	if (o instanceof BindVariable) {
    		return ObjectUtil.equals(((BindVariable)o).value, value);
    	}else {
    		return ObjectUtil.equals(o, value);
    	}
    }
    
    @Override
    public int hashCode(){
        return value != null ? value.hashCode() : 0;
    }

    public int getJdbcType() {
        return ref.getJdbcType();
    }

    public Object getValue() {
        return value;
    }
    
    public StringReader getCharacterInputStream(){ 
    	int jdbcType = getJdbcType();
    	if ( jdbcType != Types.LONGVARCHAR && jdbcType != Types.CLOB ){
    		return null;
    	}
    	return (StringReader)ref.getTypeConverter().valueOf(value);
    }

    public ByteArrayInputStream getBinaryInputStream(){ 
    	int jdbcType = getJdbcType();
    	if ( jdbcType != Types.LONGVARBINARY && jdbcType != Types.BLOB && jdbcType != Types.BINARY ){
    		return null;
    	}
    	return (ByteArrayInputStream)ref.getTypeConverter().valueOf(value);
    }

    public void encrypt(){
    	if (value != null && value instanceof String){
			value = SharedKeys.getInstance().encrypt((String)value);
		}
	}

}
