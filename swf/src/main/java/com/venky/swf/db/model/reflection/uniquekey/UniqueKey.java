package com.venky.swf.db.model.reflection.uniquekey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

public class UniqueKey<M extends Model> {
	public UniqueKey(Class<M> modelClass,String keyName){
		this.modelClass = modelClass;
		this.keyName = keyName;
	}
	
	private final Class<M> modelClass;
	public Class<M> getModelClass() {
		return this.modelClass;
	}
	
	private final String keyName ; 
	public String getKeyName() {
		return this.keyName;
	}

	public ModelReflector<M> getReflector() {
		return ModelReflector.instance(modelClass);
	}

	private Map<String,UniqueKeyFieldDescriptor<M>> fields = new HashMap<String, UniqueKeyFieldDescriptor<M>>();
	public void addField(String field, boolean allowMultipleRecordsWithNull) {
		UniqueKeyFieldDescriptor<M> ukfd = new UniqueKeyFieldDescriptor<M>(this,field);
		ukfd.setMultipleRecordsWithNullAllowed(allowMultipleRecordsWithNull);
		this.fields.put(field, ukfd);
		
	}
	
	public UniqueKeyFieldDescriptor<M> getDescriptor(String field) {
		return this.fields.get(field);
	}
	
	public int size(){
		return fields.size();
	}
	
	public Collection<UniqueKeyFieldDescriptor<M>> getFields(){
		return fields.values();
	}

}
