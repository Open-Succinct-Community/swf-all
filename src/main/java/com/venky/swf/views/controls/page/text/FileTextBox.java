package com.venky.swf.views.controls.page.text;

import com.venky.swf.views.controls.Control;
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

	public void setContentType(String contentType){
		setProperty("accept", contentType);
	}
	public String getContentType(){
		return getProperty("accept");
	}
	
	private Image image = null ;
	private Link link = null;
	public void setUrl(String url){
		if (getContentType() != null && getContentType().startsWith("image")){
			image = new Image(url);
		}else {
			link = new Link(url);
		}
	}
	 @Override
    protected void setParent(Control parent){
        super.setParent(parent);
        if (image != null){
        	parent.addControl(image);
        }
        if (link != null){
        	parent.addControl(link);
        }
    }    
}
