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
        this(imageUrl,imageUrl.endsWith("svg"));
    }
	
	private Image(String imageUrl, boolean isSvg){
		super(isSvg ? "object" : "img");
		String urlProperty = "src";
		if (isSvg){
			setProperty("type","image/svg+xml");
			urlProperty = "data";
		}
		setProperty(urlProperty, imageUrl);
	}
}
