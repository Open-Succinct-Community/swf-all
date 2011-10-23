/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

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
    
    public void setVisible(final boolean visible){
        setProperty("type", visible ? getInputType() : "hidden");
    }

    public boolean isVisible(){ 
        return !ObjectUtil.equals("hidden",getProperty("type"));
    }
    
    protected abstract String getInputType();
}
