/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.validations.Mandatory;

/**
 *
 * @author venky
 */
public class MandatoryValidator extends FieldValidator<Mandatory> {

    @Override
    public Class<Mandatory> getAnnotationClass() {
        return Mandatory.class;
    }

    @Override
    public boolean validate(Mandatory annotation, String value,StringBuilder message) {
        if ( value.length() == 0 ){
            message.append("Mandatory field");
            return false;
        }
        return true;
    }

    
}
