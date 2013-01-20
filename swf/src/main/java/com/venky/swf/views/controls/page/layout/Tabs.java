package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.UnorderedList;

public class Tabs extends Div{

	private static final long serialVersionUID = -336371552638708499L;
	private UnorderedList ul = new UnorderedList();
	public Tabs(){
		super();
		addControl(ul);
	}
	
	public void addSection(Div tab,String tabName){
		addControl(tab);
		Link link = new Link("#"+tab.getProperty("id"));
		link.setText(tabName);
		ul.createListItem().addControl(link);
	}
	

}
