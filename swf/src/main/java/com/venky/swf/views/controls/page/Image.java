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
public class Image extends Control{
    /**
	 * 
	 */
	private static final long serialVersionUID = -2715552311460845810L;

	public Image(String imageUrl){
        super("img");
        setProperty("src", imageUrl);
    }
    
}
