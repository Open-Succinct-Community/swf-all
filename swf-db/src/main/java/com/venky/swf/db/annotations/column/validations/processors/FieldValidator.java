/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelInvocationHandler;
import com.venky.swf.db.table.Record;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public abstract class FieldValidator<T extends Annotation>  {

	private String pool;
	public FieldValidator(String pool){
		this.pool = pool;
	}
	public String getPool(){
		return pool;
	}
	@SuppressWarnings("unchecked")
	public Class<T> getAnnotationClass(){
		ParameterizedType pt = (ParameterizedType)this.getClass().getGenericSuperclass();
		return (Class<T>) pt.getActualTypeArguments()[0];
	}
	
    public <M extends Model> boolean isValid(M m, String field,MultiException fieldException){
    	ModelInvocationHandler h = (ModelInvocationHandler)Proxy.getInvocationHandler(m);
    	ModelReflector<M> reflector = h.getReflector();
    	
        Method getter = reflector.getFieldGetter(field);

        Method referredModelGetter = reflector.getReferredModelGetterFor(getter);
        
        String humanizedFieldName = StringUtil.camelize(field);
        
    	if ( referredModelGetter != null){
    		humanizedFieldName = reflector.getReferredModelClass(referredModelGetter).getSimpleName();
    	}
    	

        T annotation = h.getReflector().getAnnotation(getter, getAnnotationClass());
        if (annotation == null ){
            return true;
        }

    	Record record = m.getRawRecord();
        Object value = record.get(h.getReflector().getColumnDescriptor(field).getName());
        return validate(annotation, humanizedFieldName, StringUtil.valueOf(value),fieldException);
        
    }
    public abstract boolean validate(T annotation, String humanizedFieldName, String value, MultiException fieldException);
    
    
    public static class FieldValidationException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6723774187354371203L;
    	public FieldValidationException(){
    		super();
    	}
    	
    	public FieldValidationException(String message){
    		super(message);
    	}
    }
}
