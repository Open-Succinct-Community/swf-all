/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.swf.db.annotations.column.validations.RegEx;

import java.util.regex.Pattern;

/**
 *
 * @author venky
 */
public class RegExValidator extends FieldValidator<RegEx>{

    @Override
    public Class<RegEx> getAnnotationClass() {
        return RegEx.class;
    }

    @Override
    public boolean validate(RegEx annotation, String value,StringBuilder message) {
        Pattern pattern = Pattern.compile(annotation.value());
        if (pattern.matcher(value).matches()){
            return true;
        }
        message.append(" Does not match regex :").append(annotation.value());
        return false;
    }

    
}
