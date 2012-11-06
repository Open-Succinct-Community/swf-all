/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.venky.cache.Cache;
import com.venky.core.checkpoint.Mergeable;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

/**
 *
 * @author venky
 */
public class Record implements Comparable<Record>, Cloneable , Mergeable<Record>{
    private IgnoreCaseMap<Object> fieldValues = new IgnoreCaseMap<Object>();
    private IgnoreCaseMap<Object> dirtyFields = new IgnoreCaseMap<Object>();

    @Override
    public Record clone(){
    	Record r;
		try {
			r = (Record)super.clone();
			r.fieldValues = (IgnoreCaseMap<Object>)fieldValues.clone();
			r.dirtyFields = (IgnoreCaseMap<Object>)dirtyFields.clone(); 
			r.proxyCache = new ProxyCache(r);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Should have not happened",e);
		}
    	
    	return r;
    }
    
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
    public void merge(Record record){
    	if (ObjectUtil.equals(getId(),record.getId())){
			this.fieldValues.clear(); 
	    	this.fieldValues.putAll(record.fieldValues);
	
	    	this.dirtyFields.clear();
			this.dirtyFields.putAll(record.dirtyFields);
			
			this.locked = record.locked;
			this.newRecord = record.newRecord;
    	}else {
    		throw new MergeFailedException("Cannot merge with a different record");
    	}
		//this.proxyCache left out intentionally as it is only a cache and the cache on the current object is what we want anyway 
		//and not the one on passed record.
    }
    public void load(ResultSet rs) throws SQLException{
    	load(rs,null);
    }
    public void load(ResultSet rs,ModelReflector<? extends Model> reflector) throws SQLException{
        ResultSetMetaData meta = rs.getMetaData(); 
        for (int i = 1 ; i <= meta.getColumnCount() ; i ++ ){
        	Object columnValue = rs.getObject(i);
        	String columnName = meta.getColumnName(i);
        	
        	
        	int type = meta.getColumnType(i);
        	if (JdbcTypeHelper.isLOB(type)){
				columnValue = Database.getJdbcTypeHelper().getTypeRef(type).getTypeConverter().valueOf(columnValue);
        	}else if (columnValue != null && reflector != null && type != Types.VARCHAR){ //SQLParser has similar code.
        		List<TypeRef<?>> refs = Database.getJdbcTypeHelper().getTypeRefs(type);
        		TypeRef<?> ref = null;
        		if (refs.size() == 1){
        			ref = refs.get(0);
        		}else  {
        			ref = Database.getJdbcTypeHelper().getTypeRef(reflector.getFieldGetter(reflector.getFieldName(columnName)).getReturnType());
        		}
        		columnValue = ref.getTypeConverter().valueOf(columnValue);
        	}
            put(columnName,columnValue);
        }
    }
    
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
    		return fakeId.addAndGet(1);
    	}
    }
    
    private static AtomicInteger fakeId = new AtomicInteger();
    
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
    
	
	private static class ProxyCache extends Cache<Class<? extends Model>,Model> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1436596145516567205L;
		Record record;
		ProxyCache(Record record){
			this.record = record;
		}
		@Override
		protected Model getValue(Class<? extends Model> k) {
			return ModelInvocationHandler.getProxy(k, record);
		}
	}
	private transient ProxyCache proxyCache = new ProxyCache(this);
	@SuppressWarnings("unchecked")
	public <M extends Model> M getAsProxy(Class<M> modelClass){
		return (M)proxyCache.get(modelClass);
	}
	
	private boolean locked = false;
	public boolean isLocked(){
		return locked;
	}
	public void setLocked(boolean locked){
		this.locked = locked;
	}
}
