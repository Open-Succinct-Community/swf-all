package com.venky.swf.views.controls.page;

import com.venky.xml.XMLDocument;

public class LinkedImage extends Link{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2392370091189012378L;

	public LinkedImage(String imageSource, String linkUrl){
		super(linkUrl);
		if (!imageSource.endsWith(".svg")){
			addControl(new Image(imageSource));	// Link for svg does not work with object tag
		}else {
			XMLDocument svg = XMLDocument.getDocumentFor(getClass().getResourceAsStream(imageSource.substring("/resources".length())));
			setText(svg.toString(),false);
		}
		addClass("btn");
	}
	
}
