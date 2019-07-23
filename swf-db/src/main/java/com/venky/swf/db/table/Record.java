/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import com.venky.cache.Cache;
import com.venky.core.checkpoint.Mergeable;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.util.ChangeListener;
import com.venky.core.util.ChangeObservable;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author venky
 */
public class Record implements Comparable<Record>, Cloneable , Mergeable<Record>{
    private IgnoreCaseMap<Object> fieldValues = new IgnoreCaseMap<Object>();
    private IgnoreCaseMap<Object> dirtyFields = new IgnoreCaseMap<Object>();

	private String pool;
	public Record(){
		
	}
	public void setPool(String pool){
		this.pool = pool;
	}
	public Record(String pool){
		this.pool = pool;
	}
	public String getPool(){
		return pool;
	}
    @Override
    public Record clone(){
    	Record r;
		try {
			r = (Record)super.clone();
			r.fieldValues = fieldValues.clone();
			r.dirtyFields = dirtyFields.clone();
			r.proxyCache = new ProxyCache(r);
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Should have not happened",e);
		}
    	
    	return r;
    }
    public String toString(){
    	return fieldValues.toString();
    }
    public Set<String> getFieldNames(){
        return fieldValues.keySet();
    }
    private boolean equals(Object o1,Object o2){
    	boolean ret = false ; 
    	if (o1 == o2){
    		ret = true;
    	}else if (o1 != null){
    		if (o1.equals(o2)){
    			ret = true;
    		}else if (o2 != null){
    			if (o1.getClass() != o2.getClass()){
        			BindVariable b1 = new BindVariable(getPool(),o1);
        			BindVariable b2 = new BindVariable(getPool(),o2);
        			ret = b1.getValue().equals(b2.getValue()); // May be they are equal in db terms as the underlying db types for the 2 classes are same.
    			}
    		}
    	}
    	return ret ;
    }
    protected void markDirty(String fieldName, Object oldValue, Object newValue){
    	if (isFieldDirty(fieldName)){//if already dirty..
    		Object oldestValue = dirtyFields.get(fieldName);
    		if (equals(oldestValue,newValue)){ // Value is rolled back.
    			dirtyFields.remove(fieldName);
    		}
		}else if (!equals(oldValue,newValue)){
			dirtyFields.put(fieldName,oldValue);
		}
    }
    /** 
     * 
     * @param fieldName
     * @param value
     * @return previous value of the field.
     */
    public Object put( String fieldName, Object value){
        Object oldValue =  get(fieldName); 

        //Reading Models with fields made blank don't end up being dirty!! Bug.
        if (value == null || !equals(oldValue, value)){
        	markDirty(fieldName, oldValue, value);
        	if (value != null && value instanceof ChangeObservable){
            	((ChangeObservable)value).registerChangeListener(new FieldChangeListener(this,fieldName));
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
        	String columnName = meta.getColumnLabel(i);
        	
        	
        	int type = meta.getColumnType(i);
        	if (JdbcTypeHelper.isLOB(type)){
				columnValue = Database.getJdbcTypeHelper(getPool()).getTypeRef(type).getTypeConverter().valueOf(columnValue);
        	}else if (columnValue != null && reflector != null && type != Types.VARCHAR){ //SQLParser has similar code.
        		List<TypeRef<?>> refs = Database.getJdbcTypeHelper(getPool()).getTypeRefs(type);
        		TypeRef<?> ref = null;
        		if (refs.size() == 1){
        			ref = refs.get(0);
        		}else  {
        			String fieldName = reflector.getFieldName(columnName);
        			if (fieldName == null) {
        				continue;
        			}
        			ref = Database.getJdbcTypeHelper(getPool()).getTypeRef(reflector.getFieldGetter(fieldName).getReturnType());
        		}
        		columnValue = ref.getTypeConverter().valueOf(columnValue);
        	}
            put(columnName,columnValue);
        }
        startTracking();
    }
    
    boolean newRecord = false;
    void startTracking(){
    	startTracking(true);
	}
    void startTracking(boolean clearDirtyFields){
    	if (clearDirtyFields) {
			dirtyFields.clear();
		}
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
    
    public Long getId(){
    	if (fieldValues.containsKey("ID")){
    		return Long.valueOf(String.valueOf(fieldValues.get("ID")));
    	}else {
    		return fakeId.addAndGet(1);
    	}
    }
    
    private static AtomicLong fakeId = new AtomicLong();
    
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

	public void load(Record from){
	    for (String f : from.getFieldNames()){
	        put(f,from.get(f));
        }
    }
}
