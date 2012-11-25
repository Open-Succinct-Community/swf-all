/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.ExactLength;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public class ExactLengthValidator extends FieldValidator<ExactLength>{


    @Override
    public boolean validate(ExactLength annotation, String value, MultiException ex){
        if (ObjectUtil.isVoid(value) || annotation.value() == value.length()){
            return true;
        }
        ex.add(new FieldValidationException("Expected data length :" + annotation.value() + " Actual data length :" + value.length()));
        return false;
        
    }


    
}
