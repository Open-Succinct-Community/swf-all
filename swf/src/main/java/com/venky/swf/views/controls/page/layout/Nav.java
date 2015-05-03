package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Menu;
import com.venky.swf.views.controls.page.UnorderedList;

public class Nav extends Control{

	private UnorderedList ul = new UnorderedList();
	public Nav() {
		super("nav");
		addClass("navbar navbar-default");
		
		Div fluidContainer = new Div();
		fluidContainer.addClass("container-fluid");
		addControl(fluidContainer);
		
		ul.addClass("nav navbar-nav");
		addControl(ul);
		
	}
	
	public void addMenu(Menu menu){
		ul.addControl(menu);
	}
	
	private static final long serialVersionUID = 801018969454023032L;

}
