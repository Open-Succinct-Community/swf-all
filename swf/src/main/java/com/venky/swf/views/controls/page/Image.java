/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.core.util.ObjectUtil;
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
		this(imageUrl,"");
	}
	public Image(String imageUrl,String title){
        this(imageUrl,imageUrl.endsWith("svg"));
        if (!ObjectUtil.isVoid(title)){
        	this.setProperty("title", title);
        }
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
