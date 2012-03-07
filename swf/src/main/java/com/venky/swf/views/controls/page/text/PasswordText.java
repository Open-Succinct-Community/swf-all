/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

/**
 *
 * @author venky
 */
public class PasswordText extends Input{
    /**
	 * 
	 */
	private static final long serialVersionUID = 4065251482213321895L;

	public PasswordText(){
        super();
    }

    @Override
    protected String getInputType() {
        return "password";
    }
}
