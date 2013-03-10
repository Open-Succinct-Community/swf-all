/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.exceptions.MultiException;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page._IMenu;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.LineBreak;

/**
 *
 * @author venky
 */
public class DashboardView extends HtmlView{
	RuntimeException ex = null;
    public DashboardView(Path path){
        super(path);
        ex = new RuntimeException();
    }
    
    private List<HtmlView> children = new ArrayList<HtmlView>();
    public DashboardView addChildView(HtmlView child){
    	if (child instanceof DashboardView){
    		MultiException ex = new MultiException("Multiple Dashboard views added");
    		ex.add(((DashboardView)child).ex);
    		throw ex;
    	}
        children.add(child);
        return this;
    }
    @Override
    public void createBody(_IControl b) {
    	createBody(b,true);
    }
    public void createBody(_IControl b, boolean includeMenu) {
    	if (includeMenu){
            _IMenu menu = Config.instance().getMenuBuilder().createAppMenu(getPath());
            if (menu != null && !menu.getContainedControls().isEmpty()){
                Div nav = new Div();
                nav.setProperty("class", "nav");
                nav.addControl(menu);
                b.addControl(nav);
                
            }
    	}
        for (HtmlView child:children){
            b.addControl(new LineBreak());
            child.createBody(b);
        }
    }
}
