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
public class Form extends Control{
    /**
	 * 
	 */
	private static final long serialVersionUID = -2737090749802967916L;
	public Form(){
        super("form");
    }
    public void setAction(String controllerPath, String action){
        setProperty("action", controllerPath + "/" + action);
    }
    public void setMethod(SubmitMethod method){
        setProperty("method", method == SubmitMethod.GET ? "GET" : "POST");
    }
    public static enum SubmitMethod {
        POST,
        GET
    }
}
