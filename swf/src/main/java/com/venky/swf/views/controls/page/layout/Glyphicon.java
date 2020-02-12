package com.venky.swf.views.controls.page.layout;

import java.util.HashMap;
import java.util.Map;

public class Glyphicon extends Span{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3052651954036937261L;

	static Map<String, String> bs4Map = new HashMap<>();
	static {
		bs4Map.put("glyphicon-alert","fa-exclamation-triangle");
		bs4Map.put("glyphicon-eye-open","fa-eye");
		bs4Map.put("glyphicon-duplicate","fa-clone");
		bs4Map.put("glyphicon-floppy-disk","fa-save");
		bs4Map.put("glyphicon-remove","fa-ban");
                bs4Map.put("glyphicon-refresh","fa-redo");

	}
	public String getFAIcon(String glyphicon){
		return bs4Map.getOrDefault(glyphicon,glyphicon.replace("glyphicon","fa"));
	}
	public Glyphicon(String glyphicon,String tooltip){
		addClass("fas");
		//addClass(getFAIcon("glyphicon"));
		addClass(getFAIcon(glyphicon));
		setToolTip(tooltip);
	}
}
