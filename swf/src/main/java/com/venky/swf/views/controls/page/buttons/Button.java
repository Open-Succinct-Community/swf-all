/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.buttons;

import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class Button extends Control {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7093207099887224426L;

	public Button(){
        super("input");
        setProperty("type", "button");
    }
}
