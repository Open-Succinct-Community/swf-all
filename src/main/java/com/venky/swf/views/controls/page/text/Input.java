/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public abstract class Input extends Control{


    public Input(final String... pairs){
        super("input",pairs);
        setVisible(true);
        setEnabled(true);
    }
    public final void setEnabled(final boolean enabled){
        if (enabled){
            remove("disabled");
        }else {
            setProperty("disabled", !enabled);
        }
    }
    
    public final void setVisible(final boolean visible){
        setProperty("type", visible ? getInputType() : "hidden");
    }

    public String getName(){ 
        return getProperty("name");
    }

    public void setName(final String name){
        setProperty("name", name);
    }
    
    public void setValue(final Object value){
        setProperty("value", StringUtil.valueOf(value));
    }
    
    public String getValue(){
        return getProperty("value");
    }
    
    public boolean isVisible(){ 
        return !ObjectUtil.equals("hidden",getProperty("type"));
    }
    
    public boolean isEnabled(){
        return !containsKey("disabled");
    }
    
    protected abstract String getInputType();
}
