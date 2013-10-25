package com.venky.swf.db.model.reflection.uniquekey;

import java.lang.reflect.Method;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;

public class UniqueKeyFieldDescriptor<M extends Model> {
	private final String fieldName ;
	
	public String getFieldName() {
		return this.fieldName;
	}

	private final ModelReflector<? extends Model> referredModelReflector; 
	public ModelReflector<? extends Model> getReferredModelReflector() {
		return this.referredModelReflector;
	}
	
	public UniqueKeyFieldDescriptor(UniqueKey<M> key,String fieldName){
		this.fieldName = fieldName;
		
		Method fieldGetter = key.getReflector().getFieldGetter(fieldName);
		Method referredModelGetter = key.getReflector().getReferredModelGetterFor(fieldGetter);

		if (referredModelGetter != null){
			Class<? extends Model> referredModelClass = key.getReflector().getReferredModelClass(referredModelGetter);
			referredModelReflector = ModelReflector.instance(referredModelClass) ;
		}else {
			referredModelReflector = null;
		}
	}

	private boolean multipleRecordsWithNullAllowed = true;
	public void setMultipleRecordsWithNullAllowed(
			boolean allowMultipleRecordsWithNull) {
		this.multipleRecordsWithNullAllowed = allowMultipleRecordsWithNull;
	}
	
	public boolean isMultipleRecordsWithNullAllowed(){
		return this.multipleRecordsWithNullAllowed;
	}
	
	private boolean exportable = true; 
	public boolean isExportable(){
		return exportable;
	}
	
	public void setExportable(boolean exportable){
		this.exportable = exportable;
	}
	
}
