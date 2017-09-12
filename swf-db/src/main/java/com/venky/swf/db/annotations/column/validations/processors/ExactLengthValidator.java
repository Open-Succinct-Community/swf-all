/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.ExactLength;

/**
 *
 * @author venky
 */
public class ExactLengthValidator extends FieldValidator<ExactLength>{


    public ExactLengthValidator(String pool) {
        super(pool);
    }

    @Override
    public boolean validate(ExactLength annotation, String humanizedFieldName, String value, MultiException ex){
        if (ObjectUtil.isVoid(value) || annotation.value() == value.length()){
            return true;
        }
        ex.add(new FieldValidationException(humanizedFieldName + " must be of length " + annotation.value() + " but current length is " + value.length()));
        return false;
        
    }


    
}
