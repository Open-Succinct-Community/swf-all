package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

public class UnorderedList extends Control{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7685318379631686931L;

	public UnorderedList(){
		super("ul");
	}
	
	public ListItem createListItem(){
		ListItem li = new ListItem();
		addControl(li);
		return li;
	}
	
	public static class ListItem extends Control {
		private static final long serialVersionUID = 6064137514555742818L;

		public ListItem(){
			super("li");
		}
	}
}
