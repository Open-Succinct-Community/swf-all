/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.validations.MaxLength;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public class MaxLengthValidator extends FieldValidator<MaxLength>{

    @Override
    public boolean validate(MaxLength rule, String value, MultiException ex){
        if (rule.value() >= value.length() ){
            return true;
        }
        ex.add(new FieldValidationException("Field length cannot exceed "+ rule.value()));
        return false;
    }

    
}
