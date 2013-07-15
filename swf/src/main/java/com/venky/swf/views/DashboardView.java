/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page._IMenu;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.LineBreak;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;

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
    
    HtmlView child = null;
    public void setChildView(HtmlView child){
    	if (child instanceof DashboardView){
    		MultiException ex = new MultiException("Multiple Dashboard views added");
    		ex.add(((DashboardView)child).ex);
    		throw ex;
    	}
        this.child = child;
    }
    @Override
    protected void _createBody(_IControl b, boolean includeStatusMessage) {
    	createBody(b,includeStatusMessage,true);
    }
    @Override
    protected void createBody(_IControl b) {
    	createBody(b,false, true);
    }
    public void createBody(_IControl b, boolean includeStatusMessage, boolean includeMenu) {
    	createBody(b, includeStatusMessage , includeMenu, new SequenceSet<HotLink>());
    }
    public void createBody(_IControl b, boolean includeStatusMessage,boolean includeMenu,SequenceSet<HotLink> excludeLinks) {
    	if (includeMenu){
            _IMenu menu = Config.instance().getMenuBuilder().createAppMenu(getPath());
            if (menu != null && !menu.getContainedControls().isEmpty()){
                Div nav = new Div();
                nav.setClass("nav");
                nav.addControl(menu);
                b.addControl(nav);
            }
    	}
    	if (child == null){
    		return;
    	}
    	
    	Table hotlinks = new Table();
    	hotlinks.addClass("hotlinks");
    	Column hotlinksCell = hotlinks.createRow().createColumn();
    	b.addControl(hotlinks);
    	b.addControl(new LineBreak());
		for (_IControl link : child.getHotLinks()){
			if (!excludeLinks.contains(link)){
	        	hotlinksCell.addControl(link);
			}
		}
        child._createBody(b,includeStatusMessage);
    }
}
