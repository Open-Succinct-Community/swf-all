/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.util.ArrayList;
import java.util.List;

import com.venky.swf.menu.AbstractMenuBuilderFactory;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Menu;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.LineBreak;

/**
 *
 * @author venky
 */
public class DashboardView extends HtmlView{
    public DashboardView(Path path){
        super(path);
    }
    
    private List<HtmlView> children = new ArrayList<HtmlView>();
    public DashboardView addChildView(HtmlView child){
        children.add(child);
        return this;
    }
    @Override
    protected void createBody(Body b) {
        Menu menu = AbstractMenuBuilderFactory.getMenuBuilder().createAppMenu(getPath());
        if (menu != null){
            Div nav = new Div();
            nav.setProperty("class", "nav");
            nav.addControl(menu);
            b.addControl(nav);
            
        }
        for (HtmlView child:children){
            b.addControl(new LineBreak());
            child.createBody(b);
        }
    }
    
}
