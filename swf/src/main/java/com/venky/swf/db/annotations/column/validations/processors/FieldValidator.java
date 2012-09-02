/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.lang.annotation.Annotation;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;

/**
 *
 * @author venky
 */
public abstract class FieldValidator<T extends Annotation>  {
    public boolean isValid(Annotation annotation, Object value,StringBuilder message){
        if (annotation == null ){
            return true;
        }
        if (ObjectUtil.isVoid(value)){
        	return true;
        }
        T t  = getAnnotationClass().cast(annotation);
    	return validate(t, StringUtil.valueOf(value),message);
    }
    public abstract Class<T> getAnnotationClass();
    public abstract boolean validate(T annotation, String value,StringBuilder message);
}
