/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class Script extends Control{
    public Script(){
        this(null);
    }
    public Script(String source){
        super("script");
        if (source != null){
            setProperty("type", "text/javascript");
            setProperty("src", source);
        }
    }
}
