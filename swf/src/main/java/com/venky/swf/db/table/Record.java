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
import java.util.SortedSet;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
import com.venky.core.util.ObjectUtil;

/**
 *
 * @author venky
 */
public class Record {
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
        Object oldValue = get(fieldName);
        if (!ObjectUtil.equals(oldValue, value)){
            dirtyFields.add(fieldName);
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
            put(meta.getColumnName(i),rs.getObject(i));
        }
    }
    
    private SortedSet<String> dirtyFields = new IgnoreCaseSet();
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

    public SortedSet<String> getDirtyFields() {
        return dirtyFields;
    }
    
    
    
    
}
