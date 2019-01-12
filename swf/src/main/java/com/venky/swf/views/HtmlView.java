/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;


import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.collections.UpperCaseStringCache;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.*;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.LineBreak;
import com.venky.swf.views.controls.page.layout.Paragraph;

/**
 *
 * @author venky
 */
public abstract class HtmlView extends View{
    public HtmlView(_IPath path){
        super(path);
    }
    
    private SequenceSet<HotLink> links = null; 
    public SequenceSet<HotLink> getHotLinks(){
    	if (links == null){
    	    links = new SequenceSet<HotLink>();
        	HotLink home = new HotLink("/dashboard");
        	home.addClass("home");
        	home.addControl(new Glyphicon("glyphicon-home","Home"));
        	links.add(home);

        	HotLink back = new HotLink(getPath().controllerPath() + "/back");
        	back.addClass("back");
        	back.addControl(new Glyphicon("glyphicon-arrow-left","Back"));
	        links.add(back);
    	}
    	return links;
    }

    @Override
    public void write(int httpStatusCode) throws IOException{
        HttpServletResponse response = getPath().getResponse();
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(httpStatusCode);
        response.getWriter().println("<!DOCTYPE html>");
        response.getWriter().println(this);
    }
    
    @Override
    public String toString(){
        Html html = new Html();
        SWFLogger cat = Config.instance().getLogger(getClass().getName());
        Timer htmlCreation = cat.startTimer("html creation.");
        try {
        	createHtml(html);
        }finally {
        	htmlCreation.stop();
        }

        Timer htmlToString = cat.startTimer("html rendering.");
        try {
        	return html.toString();
        }finally {
        	htmlToString.stop();
        }
    }


    protected void createHtml(Html html){
        
    	Head head = new Head();
        html.addControl(head);
        _createHead(head);

        Body body = new Body();
        html.addControl(body);
        _createBody(body,true);

        Registry.instance().callExtensions("finalize.view" + getPath().getTarget() ,  this , html);
    }
    
    private Paragraph status = new Paragraph();
    
    public static enum StatusType {
    	
    	ERROR(){
    		public String toString(){
    			return "error alert alert-warning";
    		}
    	},
    	INFO() {
    		public String toString(){
    			return "info alert alert-success";
    		}
    	};
    	public String getSessionKey(){
    		return "ui."+ toString() + ".msg";
    	}
    }
    
    public void setStatus(StatusType type, String text){
    	if (ObjectUtil.isVoid(text)){
    		return;
    	}
    	this.status.addClass(type.toString());
    	String statusText = this.status.getText();
    	if (!ObjectUtil.isVoid(statusText)){
        	statusText += "<br/>" ;
    	}
		statusText += text;
    	
		this.status.setText(statusText);
	}
    protected void createHead(Head head){

    }
    protected void _createHead(Head head){
        head.addControl(new Script("/resources/scripts/jquery/js/jquery.min.js"));

        head.addControl(new Css("/resources/scripts/bootstrap/css/bootstrap.min.css"));
        head.addControl(new Script("/resources/scripts/bootstrap/js/bootstrap.min.js"));

        head.addControl(new Css("/resources/scripts/jquery.tablesorter/css/theme.bootstrap.min.css"));
        head.addControl(new Script("/resources/scripts/jquery.tablesorter/js/jquery.tablesorter.min.js"));
        head.addControl(new Script("/resources/scripts/jquery.tablesorter/js/jquery.tablesorter.widgets.min.js"));

        
        head.addControl(new Script("/resources/scripts/bootstrap-ajax-typeahead/js/bootstrap-typeahead.min.js"));
        
        head.addControl(new Script("/resources/scripts/moment/js/moment.min.js"));
        head.addControl(new Css("/resources/scripts/eonasdan-bootstrap-datetimepicker/css/bootstrap-datetimepicker.min.css"));
        head.addControl(new Script("/resources/scripts/eonasdan-bootstrap-datetimepicker/js/bootstrap-datetimepicker.min.js"));
        
        
        
        head.addControl(new Css("/resources/scripts/swf/css/swf.css"));
        head.addControl(new Script("/resources/scripts/swf/js/swf.js"));

        String applicationInitScript = Config.instance().getProperty("swf.application.init.script", "/resources/scripts/application.js");
        if ( !applicationInitScript.startsWith("/") ) {
            applicationInitScript = "/" + applicationInitScript;
        }

        if ( applicationInitScript.startsWith("/resources")) {
            applicationInitScript = applicationInitScript.substring("/resources".length());
        }

        URL r = getClass().getResource(applicationInitScript);
        if (r != null){
            head.addControl(new Script("/resources" + applicationInitScript));
        }

        addProgressiveWebAppLinks(head);
        createHead(head);
        Registry.instance().callExtensions("after.create.head."+getPath().controllerPathElement()+"/"+getPath().action(), getPath(), head);
    }
    public void addProgressiveWebAppLinks(Head head){
        URL r = getClass().getResource("/web_manifest/manifest.json");
        if (r == null){
            return;
        }

        Link link  = new HLink("/resources/web_manifest/manifest.json");
        link.setProperty("rel","manifest");
        head.addControl(link);

        link  = new HLink("/resources/web_manifest/manifest.png"); //192x192
        link.setProperty("rel","icon");
        head.addControl(link);

        link  = new HLink("/resources/web_manifest/manifest.png");
        link.setProperty("rel","apple-touch-icon");
        head.addControl(link);


        head.addControl(new Meta("mobile-web-app-capable" , "yes"));
        head.addControl(new Meta("apple-mobile-web-app-capable" , "yes"));

        String applicationName = Config.instance().getProperty("swf.application.name", "Application");

        head.addControl(new Meta("application-name" , applicationName));
        head.addControl(new Meta("apple-mobile-web-app-title" , applicationName));

        head.addControl(new Meta("msapplication-starturl","/"));
        head.addControl(new Meta("viewport","width=device-width, initial-scale=1, shrink-to-fit=no"));
        //head.addControl(new Meta( "Service-Worker-Allowed" , "yes"));


    }
    /*
     * When views are composed, includeStatusMessage is passed as false so that it may be included in parent/including view
     */
    protected void _createBody(_IControl body,boolean includeStatusMessage){
    	int statusMessageIndex = body.getContainedControls().size();
    	showErrorsIfAny(body,statusMessageIndex, includeStatusMessage);
    	createBody(body);
    }
    protected Div getStatus(){
    	FluidContainer container = new FluidContainer();
    	Column column = container.createRow().createColumn(0, 12);
    	column.addControl(status);
    	return container;
    }
    
    @SuppressWarnings("unchecked")
	protected void showErrorsIfAny(_IControl body,int index, boolean includeStatusMessage){
    	HttpSession session = getPath().getSession();
    	if (session != null && includeStatusMessage){
    		body.addControl(index,getStatus());
			List<String> errorMessages = getPath().getErrorMessages();
			List<String> infoMessages = getPath().getInfoMessages();
			
			boolean hasError = !errorMessages.isEmpty();
            boolean addNewLine = false;
            StringBuilder message = new StringBuilder();
            for (List<String> messageList : Arrays.asList(errorMessages,infoMessages)){
    			for (String errorMsg : messageList){
    				if (addNewLine){
    					message.append(new LineBreak());	
    				}
    				message.append(errorMsg);
    				addNewLine = true;
    			}
            }
            setStatus(hasError ? StatusType.ERROR : StatusType.INFO , message.toString());
		}
    }
    public static void addHotLinks(_IControl b,  SequenceSet<HotLink> links, SequenceSet<HotLink> excludeLinks){
    	addHotLinks(b, b.getContainedControls().size(), links, excludeLinks);
    }
    public static void addHotLinks(_IControl b, int index, SequenceSet<HotLink> links, SequenceSet<HotLink> excludeLinks){
    	FluidContainer hotlinks = new FluidContainer();
    	hotlinks.addClass("hotlinks");
    	b.addControl(index,hotlinks);
    	
    	Column hotlinksCell = hotlinks.createRow().createColumn(0,12);
    	for (_IControl link : links){
			if (!excludeLinks.contains(link)){
	        	hotlinksCell.addControl(link);
			}
		}
    	
    }
    protected abstract void createBody(_IControl b);
    
}
