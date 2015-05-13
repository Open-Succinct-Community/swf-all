/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.collections.SequenceSet;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.extensions.MenuBuilderFactory;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page._IMenu;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.Nav;

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
            _IMenu menu = MenuBuilderFactory.instance().getMenuBuilder().createAppMenu(getPath());
            if (menu != null && !menu.getContainedControls().isEmpty()){
                Nav nav = new Nav();
                nav.addControl(menu);
                b.addControl(nav);
            }
    	}
    	
    	if (child == null){
    		super.showErrorsIfAny(b, b.getContainedControls().size(),includeStatusMessage);
    		return;
    	}
    	
    	
    	
    	FluidContainer hotlinks = new FluidContainer();
    	hotlinks.addClass("hotlinks");
    	b.addControl(hotlinks);
    	
    	Column hotlinksCell = hotlinks.createRow().createColumn(0,12);
    	for (_IControl link : child.getHotLinks()){
			if (!excludeLinks.contains(link)){
	        	hotlinksCell.addControl(link);
			}
		}
    	
        child._createBody(b,includeStatusMessage);
    }
}
