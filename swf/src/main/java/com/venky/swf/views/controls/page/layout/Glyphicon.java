package com.venky.swf.views.controls.page.layout;

public class Glyphicon extends Span{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3052651954036937261L;
	
	public Glyphicon(String glyphicon,String tooltip){
		addClass(glyphicon);
		setToolTip(tooltip);
	}
}
