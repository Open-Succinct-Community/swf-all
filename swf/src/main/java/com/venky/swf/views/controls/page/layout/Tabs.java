package com.venky.swf.views.controls.page.layout;

import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.text.Select;
import com.venky.swf.views.controls.page.text.Select.Option;

public class Tabs extends Div{

	private static final long serialVersionUID = -336371552638708499L;
	private Div tablinks = new Div();
	private Div content = new Div();
	private Control nav = new Control("nav");
	private Select select = new Select();
	public Tabs(){
		super();

		addControl(nav);

		nav.addControl(tablinks);
		addControl(select);

		addControl(content);
		tablinks.addClass("nav nav-tabs");
		tablinks.setProperty("role","tablist");
		content.addClass("tab-content");
		select.addClass("form-control");
	}
	public void addSection(Div tabpane,String tabName,boolean makeActive){
		Link link = new Link("#"+tabpane.getProperty("id"));
		link.setText(tabName);
		link.addClass("nav-item nav-link");
		link.setProperty("data-toggle","tab");
		link.setProperty("role","tab");
		link.setProperty("aria-controls", tabpane.getProperty("id"));
		link.setProperty("aria-selected",makeActive);

		Option option  = select.createOption(tabName,tabName);
		option.setProperty("onclick","$('#"+link.getProperty("id")+"').tab('show')");


		tablinks.addControl(link);
		content.addControl(tabpane);
		
		if (makeActive){
			link.addClass("active");
			tabpane.addClass("show active");
			option.setProperty("selected","selected");
		}

		tabpane.addClass("tab-pane fade");
		tabpane.setProperty("aria-labelledby",link.getId());
		tabpane.setProperty("role","tabpanel");

	}
	

}
