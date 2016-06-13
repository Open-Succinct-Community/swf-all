/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.exceptions.MultiException;

/**
 *
 * @author venky
 */
public class NotNullValidator extends FieldValidator<IS_NULLABLE> {

    public NotNullValidator(String pool) {
        super(pool);
    }

    @Override
    public boolean validate(IS_NULLABLE annotation,String humanizedFieldName,  String value,MultiException ex) {
        if (!annotation.value() && value.trim().length() == 0 ){
            ex.add(new FieldValidationException(humanizedFieldName + " is mandatory"));
            return false;
        }
        return true;
    }

    
}
