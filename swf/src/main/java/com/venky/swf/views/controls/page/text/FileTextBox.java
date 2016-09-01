package com.venky.swf.views.controls.page.text;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.InputGroup;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Span;


public class FileTextBox extends Input{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3299839129375841151L;
	public FileTextBox() {
		super();
		removeClass("form-control");
	}
	@Override
	protected String getInputType() {
		return "file";
	}
	
	@Override 
	public void setVisible(boolean visible){
		super.setVisible(visible);
		if (link != null){
			link.setVisible(visible);
		}
	}

	public void setContentType(String contentType){	
		setProperty("accept",contentType);
	}
	public void setContentType(MimeType contentType){
		setContentType(contentType.toString());
	}
	
	public String getContentType(){
		return getProperty("accept");
	}
	
	private Link link = null;
	public void setStreamUrl(String url,String text){
		link = new Link(url);
		link.setVisible(isVisible());
		if (getContentType() != null && getContentType().startsWith("image")){
			Image image = new Image(url);
    		link.addControl(image);
		}else {
			if (ObjectUtil.isVoid(text)){
				link.setText("here...");
			}else {
				link.setText(text);
			}
		}
		link.addClass("stream");
	}
	
	public Link getStreamLink(){
		return link;
	}
	
	public InputGroup getStylishVersion(){ 
		if (this.getParent() != null){ 
			throw new RuntimeException("Must call before adding to another control");
		}
		InputGroup ig = new InputGroup();
		Span fakeButtonGroup = new Span();
		fakeButtonGroup.addClass("input-group-btn");
		ig.addControl(fakeButtonGroup);
		
		Span fakeButton = new Span();
		fakeButton.addClass("btn btn-default btn-file");
		fakeButton.setText("Browse...");
		fakeButtonGroup.addControl(fakeButton);
		fakeButton.addControl(this);
		fakeButton.setEnabled(this.isEnabled());
		
		TextBox faketextBox =  new TextBox();
		faketextBox.setReadOnly(true);
		faketextBox.setEnabled(this.isEnabled());
		
		ig.addControl(faketextBox);
		return ig;
	}
}
