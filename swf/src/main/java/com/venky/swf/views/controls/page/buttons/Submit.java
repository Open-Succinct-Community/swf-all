/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.buttons;

/**
 *
 * @author venky
 */
public class Submit extends Button{
    /**
	 * 
	 */
	private static final long serialVersionUID = 8489233083608975839L;
	public Submit(){
        this("Ok");
    }
    public Submit(String label){
        super();
        setProperty("type", "submit");
        setProperty("value", label);
    }
}
