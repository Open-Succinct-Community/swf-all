package com.venky.swf.views.controls.page.text;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;


public class FileTextBox extends Input{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3299839129375841151L;
	
	@Override
	protected String getInputType() {
		return "file";
	}

	public void setContentType(MimeType contentType){
		setProperty("accept", contentType.toString());
	}
	public String getContentType(){
		return getProperty("accept");
	}
	
	private Link link = null;
	public void setStreamUrl(String url){
		link = new Link(url);
		if (getContentType() != null && getContentType().startsWith("image")){
			Image image = new Image(url);
    		link.addControl(image);
		}
		link.addClass("stream");
	}
	
	public Link getStreamLink(){
		return link;
	}
	
}
