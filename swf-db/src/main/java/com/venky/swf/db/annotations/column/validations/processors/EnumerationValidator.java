/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.validations.Enumeration;

/**
 *
 * @author venky
 */
@SuppressWarnings("ALL")
public class EnumerationValidator extends FieldValidator<Enumeration> {

    public EnumerationValidator(String pool) {
        super(pool);
    }

    @Override
    public  boolean validate(Enumeration annotation, String humanizedFieldName, String value, MultiException ex){
    	if (ObjectUtil.isVoid(value)){
    		return true;
    	}
        Set<String> allowedValues = new HashSet<>();
        if (!ObjectUtil.isVoid(annotation.value())) {
            StringTokenizer tokens = new StringTokenizer(annotation.value(), ",");
            while (tokens.hasMoreElements()) {
                allowedValues.add(tokens.nextToken());
            }
        }else if (!ObjectUtil.isVoid(annotation.enumClass())){
            try {
                Class<Enum> c = (Class<Enum>)Class.forName(annotation.enumClass());
                for (Enum enumConstant : c.getEnumConstants()) {
                    allowedValues.add(enumConstant.name());
                }
            }catch (Exception cce){
                throw new RuntimeException(cce);
            }
        }
        if (allowedValues.contains(value)) {
            return true;
        }
        ex.add(new FieldValidationException(humanizedFieldName + " must be one of these values." + allowedValues));
        return false;

    }

}
