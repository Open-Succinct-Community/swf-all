/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.IS_NULLABLE;

/**
 *
 * @author venky
 */
public class NotNullValidator extends FieldValidator<IS_NULLABLE> {

    @Override
    public Class<IS_NULLABLE> getAnnotationClass() {
        return IS_NULLABLE.class;
    }

    @Override
    public boolean validate(IS_NULLABLE annotation, String value,StringBuilder message) {
        if (!annotation.value() && value.trim().length() == 0 ){
            message.append("Mandatory field");
            return false;
        }
        return true;
    }

    
}
