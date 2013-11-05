/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.page._IMenu;

/**
 *
 * @author venky
 */
public interface _IMenuBuilder {
	public static final String MENU_BUILDER_OBJECT_KEY = "com.venky.swf.menu.builder";
    public _IMenu createAppMenu(_IPath path); 
}
