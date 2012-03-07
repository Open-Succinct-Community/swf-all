/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.validations.MaxLength;

/**
 *
 * @author venky
 */
public class MaxLengthValidator extends FieldValidator<MaxLength>{

    @Override
    public Class<MaxLength> getAnnotationClass() {
        return MaxLength.class;
    }

    @Override
    public  boolean validate(MaxLength rule, String value,StringBuilder message) {
        if (rule.value() >= value.length() ){
            return true;
        }
        message.append("Length cannot exceed ").append(rule.value());
        return false;
    }

    
}
