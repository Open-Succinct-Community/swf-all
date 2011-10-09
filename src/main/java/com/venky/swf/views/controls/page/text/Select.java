package com.venky.swf.views.controls.page.text;

import com.venky.core.string.StringUtil;
import com.venky.swf.views.controls.Control;

public class Select extends Control {

	public Select() {
		super("select");
	}
	
	public Option createOption(String text, Object value){
		Option option = new Option(text, value);
		addControl(option);
		return option;
	}
	
	public static class Option extends Control {

		public Option(String text,Object value) {
			super("option");
			setText(text);
			setProperty("value", StringUtil.valueOf(value));
		}
		
	}
	
}
