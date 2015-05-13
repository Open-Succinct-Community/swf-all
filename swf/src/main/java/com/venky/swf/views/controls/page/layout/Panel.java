package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls.page.layout.headings.H;

public class Panel extends Div{

	private static final long serialVersionUID = 4674913434168402240L;
	
	public Panel(){
		super();
		addClass("panel-default");
	}
	
	
	public PanelHeading createPanelHeading(){
		PanelHeading h = new PanelHeading();
		addControl(h);
		return h;
	}

	public static class PanelHeading extends Div {
		private static final long serialVersionUID = -2523459311141510390L;
		H h4 = new H(4);
		public PanelHeading(){
			super();
			addControl(h4);
		}
		public void setTitle(String title){
			h4.setText(title);
		}
	}
}
