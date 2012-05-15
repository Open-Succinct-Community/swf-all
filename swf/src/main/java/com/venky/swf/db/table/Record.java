/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;

/**
 *
 * @author venky
 */
public class Record implements Comparable<Record>{
    private Map<String,Object> fieldValues = new IgnoreCaseMap<Object>();
    
    public Set<String> getFieldNames(){
        return fieldValues.keySet();
    }
    /** 
     * 
     * @param fieldName
     * @param value
     * @return previous value of the field.
     */
    public Object put(String fieldName, Object value){
        Object oldValue =  get(fieldName); 

        if (!ObjectUtil.equals(oldValue, value)){
        	if (isFieldDirty(fieldName)){//if already dirty..
        		Object oldestValue = dirtyFields.get(fieldName);
        		if (ObjectUtil.equals(oldestValue,value)){ // Value is rolled back.
        			dirtyFields.remove(fieldName);
        		}
    		}else {
    			dirtyFields.put(fieldName,oldValue);
    		}
        }
        return fieldValues.put(fieldName, value);
    }
    public Object get(String fieldName){
        return fieldValues.get(fieldName);
    }
    
    public Object remove(String fieldName){
        return fieldValues.remove(fieldName);
    }
    
    public void load(ResultSet rs) throws SQLException{
        ResultSetMetaData meta = rs.getMetaData(); 
        for (int i = 1 ; i <= meta.getColumnCount() ; i ++ ){
        	Object columnValue = rs.getObject(i);
        	int type = meta.getColumnType(i);
        	if (JdbcTypeHelper.isLOB(type)){
				columnValue = Database.getJdbcTypeHelper().getTypeRef(type).getTypeConverter().valueOf(columnValue);
        	}
            put(meta.getColumnName(i),columnValue);
        }
    }
    
    private SortedMap<String,Object> dirtyFields = new IgnoreCaseMap();
    boolean newRecord = false; 
    void startTracking(){
        dirtyFields.clear();
        newRecord = fieldValues.isEmpty();
    }

    public boolean isNewRecord() {
        return newRecord;
    }

    public void setNewRecord(boolean newRecord) {
        this.newRecord = newRecord;
    }

    public Set<String> getDirtyFields() {
        return dirtyFields.keySet();
    }
    
    public boolean isFieldDirty(String field){
    	return dirtyFields.containsKey(field);
    }
    
    public Object getOldValue(String field){
    	return dirtyFields.get(field);
    }
    
    public Integer getId(){
    	if (fieldValues.containsKey("ID")){
    		return Integer.valueOf(String.valueOf(fieldValues.get("ID")));
    	}else {
    		return null;
    	}
    }
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getId().hashCode();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Record other = (Record) obj;
		if (fieldValues == null) {
			if (other.fieldValues != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}
	public int compareTo(Record o) {
		return this.getId().compareTo(o.getId());
	}
    
	
    
}
