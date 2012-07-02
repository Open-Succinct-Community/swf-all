/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import java.util.HashMap;
import java.util.Map;

import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class Menu extends Control implements _IMenu{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Menu(){
        super("ul");
    }
    
	public boolean isEmpty(){
		return getContainedControls().isEmpty();
	}
	
	public MenuItem createMenuItem(String text,String url){
        MenuItem mi = new MenuItem(text, url) ;
        addControl(mi);
        return mi;
    }
    
	private transient Map<String,Menu> subMenuMap = new HashMap<String, Menu>();
    public MenuItem createMenuItem(String text,Menu subMenu){
        MenuItem mi = new MenuItem(text, subMenu) ;
        addControl(mi);
        subMenuMap.put(text, subMenu);
        return mi;
    }
    public static class MenuItem extends Control { 
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public MenuItem(String text,String url){
            super("li");
            Link link = new Link();
            link.setUrl(url);
            link.setText(text);
            addControl(link);
        }
        public MenuItem(String text,Menu submenu){
            super("li");
            setText(text);
            addControl(submenu);
        }
    }
	public Menu getSubmenu(String menuName) {
		Menu subMenu = subMenuMap.get(menuName);
		if (subMenu == null){
			subMenu = new Menu();
			createMenuItem(menuName, subMenu);
		}
		return subMenu;
	}
}
