/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.venky.extension.Registry;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Css;
import com.venky.swf.views.controls.page.Head;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Script;
import com.venky.swf.views.controls.page.layout.LineBreak;
import com.venky.swf.views.controls.page.text.Label;

/**
 *
 * @author venky
 */
public abstract class HtmlView extends View{
    public HtmlView(_IPath path){
        super(path);
    }

    public void write() throws IOException{ 
        HttpServletResponse response = getPath().getResponse();
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<!DOCTYPE html>");
        response.getWriter().println(this);
    }
    
    @Override
    public String toString(){
        Html html = new Html();
        createHtml(html);
        return html.toString();
    }


    protected void createHtml(Html html){
        
    	Head head = new Head();
        createHead(head);
        html.addControl(head);
        
        Body body = new Body();
        createBody(body);
        body.addControl(new LineBreak());
        body.addControl(status);
        html.addControl(body);

        Registry.instance().callExtensions("finalize.view" + getPath().getTarget() ,  this , html);
    }
    
    private Label status = new Label(); 
    public static enum StatusType {
    	ERROR(){
    		public String toString(){
    			return "status-error";
    		}
    	},
    	INFO() {
    		public String toString(){
    			return "status-info";
    		}
    	}
    }
    
    public void setStatus(StatusType type, String text){
    	this.status.addClass(type.toString());
    	this.status.setText(text);
	}
    
    protected void createHead(Head head){
        head.addControl(new Css("/resources/scripts/jquery-ui/css/ui-lightness/jquery-ui-1.10.0.custom.css"));
        head.addControl(new Script("/resources/scripts/jquery-ui/js/jquery-1.9.0.js"));
        head.addControl(new Script("/resources/scripts/jquery-ui/js/jquery-ui-1.10.0.custom.min.js"));
        
        head.addControl(new Css("/resources/scripts/jquery.tablesorter/themes/blue/style.css"));
        head.addControl(new Script("/resources/scripts/jquery.tablesorter/jquery.tablesorter.js"));
        head.addControl(new Script("/resources/scripts/jquery.tablesorter/addons/pager/jquery.tablesorter.pager.js"));
        
        head.addControl(new Css("/resources/scripts/swf/css/swf.css"));
        head.addControl(new Script("/resources/scripts/swf/js/autocomplete.js"));
        head.addControl(new Script("/resources/scripts/swf/js/datepicker.js"));
        head.addControl(new Script("/resources/scripts/swf/js/tablesorter.js"));
        Registry.instance().callExtensions(getPath().controllerPathElement()+"/"+getPath().action(), head);
    }
    
    protected abstract void createBody(_IControl b);
    
}
