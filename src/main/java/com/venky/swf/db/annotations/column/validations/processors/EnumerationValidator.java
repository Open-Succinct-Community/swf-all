/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.annotations.column.validations.Enumeration;

import java.util.StringTokenizer;

/**
 *
 * @author venky
 */
public class EnumerationValidator extends FieldValidator<Enumeration> {

    @Override
    public Class<Enumeration> getAnnotationClass() {
        return Enumeration.class;
    }

    @Override
    public boolean validate(Enumeration annotation, String value, StringBuilder message) {
        StringTokenizer tokens = new StringTokenizer(annotation.value(),",");
        while (tokens.hasMoreElements()){
            String token = tokens.nextToken();
            if (StringUtil.equals(token, value)){
                return true;
            }
        }
        message.append(value).append(" not in (").append(annotation.value()).append( ")");
        return false;

    }
    
}
