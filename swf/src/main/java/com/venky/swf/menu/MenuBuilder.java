/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Menu;

/**
 *
 * @author venky
 */
public interface MenuBuilder {
    public Menu createAppMenu(Path path); 
}
