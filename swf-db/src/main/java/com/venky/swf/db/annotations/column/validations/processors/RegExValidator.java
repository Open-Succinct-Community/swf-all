/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.util.regex.Pattern;

import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.RegEx;

/**
 *
 * @author venky
 */
public class RegExValidator extends FieldValidator<RegEx>{


    public RegExValidator(String pool) {
        super(pool);
    }

    @Override
    public boolean validate(RegEx annotation, String humanizedFieldName, String value, MultiException ex){
        Pattern pattern = Pattern.compile(annotation.value());
        if (ObjectUtil.isVoid(value) || pattern.matcher(value).matches()){
            return true;
        }
        ex.add(new FieldValidationException(humanizedFieldName + "("+ value +")" +" doesnot match regex :" + annotation.value()));
        return false;
    }
    
}
