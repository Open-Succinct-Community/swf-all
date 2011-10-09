/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.util.HashSet;
import java.util.Set;

import com.venky.swf.db.model.Model;

/**
 *
 * @author venky
 */
public class UIModelHelper {
    private final static Set<String> defaultProtectedFields = new HashSet<String>() ;
    private final static Set<String> defaultHiddenFields = new HashSet<String>() ;
    static {
    	defaultHiddenFields.add("ID");
        defaultHiddenFields.add("VERSION");
    }
    
    
    public static Set<String> getDefaultProtectedFields(Class<? extends Model> modelClass){
        return defaultProtectedFields;
    }
    
    public static Set<String> getDefaultHiddenFields(Class<? extends Model> modelClass){
    	return defaultHiddenFields;
    }
    
}
