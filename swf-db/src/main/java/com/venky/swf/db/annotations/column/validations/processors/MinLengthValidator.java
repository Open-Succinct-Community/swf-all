/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.validations.MinLength;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public class MinLengthValidator extends FieldValidator<MinLength>{

    @Override
    public boolean validate(MinLength rule, String humanizedFieldName, String value, MultiException ex){
        if (rule.value() <= value.length() ){
            return true;
        }
        ex.add(new FieldValidationException(humanizedFieldName + " length must exceed "+ rule.value()));
        return false;
    }

    
}
