/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

/**
 *
 * @author venky
 */
public class Css extends Link{
    /**
	 * 
	 */
	private static final long serialVersionUID = 2322338613044053185L;

	public Css(String path){
        super("link",path);
        setProperty("rel", "stylesheet");
    }
}
