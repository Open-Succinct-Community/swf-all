/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.util.StringTokenizer;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public class EnumerationValidator extends FieldValidator<Enumeration> {

    @Override
    public boolean validate(Enumeration annotation, String humanizedFieldName, String value, MultiException ex){
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
