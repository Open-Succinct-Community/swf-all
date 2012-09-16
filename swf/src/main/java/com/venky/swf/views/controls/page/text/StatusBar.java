package com.venky.swf.views.controls.page.text;

public class StatusBar extends Label{

	public static enum Type { 
		ERROR(){
			public String toString(){
				return "error";
			}
		},
		INFO(){
			public String toString(){
				return "info";
			}
		}
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 6290759560772962812L;
	public StatusBar(Type statusType, String message){
		super(message);
		addClass(statusType.toString());
	}
	
}
