/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.MultiException;
import com.venky.swf.extensions.MenuBuilderFactory;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Head;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page._IMenu;
import com.venky.swf.views.controls.page.buttons.Button;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.Nav;
import com.venky.swf.views.controls.page.layout.Span;
import com.venky.swf.views.controls.page.text.Label;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author venky
 */
public class DashboardView extends HtmlView{
	RuntimeException ex = null;
    SequenceSet<HotLink> excludeLinks = new SequenceSet<>();
    public DashboardView(Path path){
        super(path);
        ex = new RuntimeException();
    }

    public void addExcludeLinks(HotLink hotLink){
        excludeLinks.add(hotLink);
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
    protected void createHead(Head head){
        super.createHead(head);
        if (child!= null){
            child.createHead(head);
        }
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
    	createBody(b, includeStatusMessage , includeMenu, excludeLinks);
    }
    public void createBody(_IControl b, boolean includeStatusMessage,boolean includeMenu,SequenceSet<HotLink> excludeLinks) {
    	if (includeMenu){
            _IMenu menu = MenuBuilderFactory.instance().getMenuBuilder().createAppMenu(getPath());
            if (menu != null && !menu.getContainedControls().isEmpty()){
                Nav nav = new Nav();
                nav.addClass("navbar navbar-expand-lg navbar-light bg-light");


                Div container = new Div();
                container.addClass("collapse navbar-collapse");

                Link logo_bar = new Link("#");
                logo_bar.addClass("navbar-brand");
                Image logo = getLogo() ;
                logo_bar.addControl(logo != null ? logo : new Label(getApplicationName()));


                Button button = new Button("button");
                button.addClass("navbar-toggler");
                button.setProperty("data-toggle","collapse");
                button.setProperty("data-target","#"+container.getId());

                Span span = new Span();
                span.addClass("navbar-toggler-icon");
                button.addControl(span);


                nav.addControl(button);
                nav.addControl(container);

                container.addControl(menu);
                b.addControl(nav);
            }
    	}
    	
    	if (child == null){
    		super.showErrorsIfAny(b, b.getContainedControls().size(),includeStatusMessage);
    		return;
    	}
    	
    	
    	
    	addHotLinks(b, child.getHotLinks(), excludeLinks);
        child._createBody(b,includeStatusMessage);
    }
    
    
}
