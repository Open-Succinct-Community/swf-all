/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.util.StringTokenizer;

import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.Enumeration;

/**
 *
 * @author venky
 */
public class EnumerationValidator extends FieldValidator<Enumeration> {

    public EnumerationValidator(String pool) {
        super(pool);
    }

    @Override
    public boolean validate(Enumeration annotation, String humanizedFieldName, String value, MultiException ex){
    	if (ObjectUtil.isVoid(value)){
    		return true;
    	}
        StringTokenizer tokens = new StringTokenizer(annotation.value(),",");
        while (tokens.hasMoreElements()){
            String token = tokens.nextToken();
            if (StringUtil.equals(token, value)){
                return true;
            }
        }
        ex.add(new FieldValidationException( humanizedFieldName + " must be one of these values. (" + annotation.value() +  ")"));
        return false;

    }

}
