package com.venky.swf.views.controls.page;

public class Icon extends Image {

	private static final long serialVersionUID = 7452955048300663718L;
	
	public Icon(String imageUrl) {
		this(imageUrl,"");
	}
	public Icon(String imageUrl, String tooltip) {
		super(imageUrl, tooltip);
		addClass("img-thumbnail");
	}


}
