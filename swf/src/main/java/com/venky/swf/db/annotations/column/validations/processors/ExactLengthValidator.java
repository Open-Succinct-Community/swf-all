/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.ExactLength;

/**
 *
 * @author venky
 */
public class ExactLengthValidator extends FieldValidator<ExactLength>{

    @Override
    public Class<ExactLength> getAnnotationClass() {
        return ExactLength.class;
    }

    @Override
    public boolean validate(ExactLength annotation, String value,StringBuilder message) {
        if (ObjectUtil.isVoid(value) || annotation.value() == value.length()){
            return true;
        }
        message.append("Does not match length ").append("Expected :").append(annotation.value()).append( " Actual :").append(value.length());
        return false;
        
    }


    
}
