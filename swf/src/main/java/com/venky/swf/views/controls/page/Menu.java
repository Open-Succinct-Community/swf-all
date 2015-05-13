/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import java.util.HashMap;
import java.util.Map;

import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.layout.Span;

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
		this(true);
    }
	private Menu(boolean isMainmenu){
		super("ul");
		if (isMainmenu){
			addClass("nav navbar-nav");
		}else {
			addClass("dropdown-menu");
			setProperty("role", "menu");
		}
	}
    
	public boolean isEmpty(){
		return getContainedControls().isEmpty();
	}
	
	public MenuItem createMenuItem(String text,String url){
		return createMenuItem(text, url,null); 
    }
	public MenuItem createMenuItem(String text,String url,Icon img){
	    MenuItem mi = new MenuItem(text, url,img) ;
	    addControl(mi);
	    return mi;
	}
	private transient Map<String,Menu> subMenuMap = new HashMap<String, Menu>();
    
	public MenuItem createMenuItem(String text,Menu subMenu){
        MenuItem mi = new MenuItem(text, subMenu) ;
        mi.addClass("dropdown");
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
            this(text,url,null);
        }
		public MenuItem(String text,String url,Icon img){
            super("li");
            Link link = new Link();
            link.setUrl(url);
            link.setText(text);
            if (img != null){
            	link.addControl(img);
            }
            addControl(link);
        }
        public MenuItem(String text,Menu submenu){
            super("li");
            addClass("dropdown");
            
            Link link = new Link();
            link.setUrl("#");
            link.setText(text);
            link.addClass("dropdown-toggle");
            link.setProperty("data-toggle", "dropdown");
            link.setProperty("role", "button");
            link.setProperty("aria-expanded", false);
            
            Span s = new Span();
            s.addClass("caret");
            
            link.addControl(s);
            addControl(link);
            addControl(submenu);
        }
    }
	public Menu getSubmenu(String menuName) {
		Menu subMenu = subMenuMap.get(menuName);
		if (subMenu == null){
			subMenu = new Menu(false);
			createMenuItem(menuName, subMenu);
		}
		return subMenu;
	}
}
