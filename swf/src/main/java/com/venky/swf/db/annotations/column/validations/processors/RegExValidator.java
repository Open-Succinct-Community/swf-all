/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.exceptions.MultiException;

import java.util.regex.Pattern;

/**
 *
 * @author venky
 */
public class RegExValidator extends FieldValidator<RegEx>{

    
    @Override
    public boolean validate(RegEx annotation, String value, MultiException ex){
        Pattern pattern = Pattern.compile(annotation.value());
        if (ObjectUtil.isVoid(value) || pattern.matcher(value).matches()){
            return true;
        }
        ex.add(new FieldValidationException(" must match regex :" + annotation.value()));
        return false;
    }
    
}
