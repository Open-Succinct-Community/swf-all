/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.cache.Cache;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.layout.Div;

import java.util.Map;

/**
 *
 * @author venky
 */
public class Menu extends Control implements _IMenu{

	public Menu() {
		super("ul");
	}

	private Map<String, SubMenu> map = new Cache<String, SubMenu>() {
		@Override
		protected SubMenu getValue(String menuName) {
			SubMenu menu = new SubMenu(menuName);
			Menu.this.addControl(menu);
			return menu;
		}
	};

	public SubMenu getSubmenu(String menuName) {
		return map.get(menuName);
	}


	public static class SubMenu extends Control {
		public SubMenu(String text){
			super("li");
			addClass("nav-item dropdown");
			Link link = new Link("#");
			link.addClass("nav-link dropdown-toggle");
			link.setProperty("role","button");
			link.setProperty("data-toggle","dropdown");
			link.setProperty("aria-haspopup",true);
			link.setProperty("aria-expanded",false);
			link.setText(text);
			addControl(link);

			submenuHolder = new Div();
			submenuHolder.addClass("dropdown-menu");
			submenuHolder.setProperty("aria-labelledby",link.getId());
			addControl(submenuHolder);
		}
		Div submenuHolder = null;
		public void addMenuItem(String text, String url) {
			Link link = new Link(url);
			link.addClass("dropdown-item");
			link.setText(text);
			submenuHolder.addControl(link);
		}
	}
}
