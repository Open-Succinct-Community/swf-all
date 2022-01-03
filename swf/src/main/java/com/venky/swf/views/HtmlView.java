/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;


import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.cache.CacheVersion;
import com.venky.swf.extensions.LastCacheVersion;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Css;
import com.venky.swf.views.controls.page.HLink;
import com.venky.swf.views.controls.page.Head;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.Meta;
import com.venky.swf.views.controls.page.Script;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.FluidContainer;
import com.venky.swf.views.controls.page.layout.FluidContainer.Column;
import com.venky.swf.views.controls.page.layout.Glyphicon;
import com.venky.swf.views.controls.page.layout.LineBreak;
import com.venky.swf.views.controls.page.layout.Paragraph;
import com.venky.swf.views.controls.page.layout.Title;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

    static JSONObject manifestJson = null;
    protected JSONObject getManifestJson(){
        if (manifestJson == null){
            URL r = getClass().getResource("/manifest.json");
            String path = "";
            if (r == null){
                r = getClass().getResource("/web_manifest/manifest.json");
                path = "/web_manifest";
            }

            if (r != null){
                try {
                    manifestJson = (JSONObject) JSONValue.parse(new InputStreamReader(r.openStream()));
                    manifestJson.put("resource_path",path);
                } catch (IOException e) {
                    manifestJson = new JSONObject();
                }
            }
        }
        return manifestJson;
    }
    @SuppressWarnings("unchecked")
    public Image getLogo(){
        JSONObject manifest = getManifestJson();
        if (manifest.get("resource_path") == null){
            return null;
        }
        String path = (String)manifest.get("resource_path");
        JSONArray icons = (JSONArray)manifest.get("icons");
        JSONObject icon = null;
        String icon_url = null;
        if (!icons.isEmpty()){
            for (Object oicon : icons){
                JSONObject jsIcon = (JSONObject)oicon;
                String sizes = (String)jsIcon.get("sizes");
                for (String sSize : sizes.split(",")){
                    String[] dims = sSize.split("x");
                    int dim = Integer.parseInt(dims[0]);
                    if (dim >= 300){
                        icon = jsIcon;
                        break;
                    }
                }
                if (icon != null){
                    break;
                }
            }
            if (icon != null) {
                icon_url = (String)icon.getOrDefault("src", null);
            }
        }
        if (icon_url == null){
            icon_url = String.format("%s/manifest.png",path);
            if (getClass().getResource(icon_url) == null){
                icon_url = null;
            }
        }

        Image image = null;
        if (!ObjectUtil.isVoid(icon_url)){
            if (icon_url.startsWith("/")){
                icon_url = Config.instance().getServerBaseUrl() + icon_url + "?v=" + LastCacheVersion.getInstance().get().getVersionNumber();
            }
            image = new Image(icon_url);
            image.addClass("w-100");
        }
        return image;
    }
    public String getApplicationName(){
        JSONObject manifest = getManifestJson();
        String defaultApplicationName = Config.instance().getProperty("swf.application.name", "My Application");

        if (manifest.get("resource_path") == null){
            return defaultApplicationName;
        }
        return (String)manifest.getOrDefault("name",defaultApplicationName);
    }
    public String getApplicationDescription(){
        JSONObject manifest = getManifestJson();
        String defaultApplicationName = Config.instance().getProperty("swf.application.description", getApplicationName());

        if (manifest.get("resource_path") == null){
            return defaultApplicationName;
        }
        return (String)manifest.getOrDefault("description",defaultApplicationName);
    }
    @Override
    public void write(int httpStatusCode) throws IOException{
        HttpServletResponse response = getPath().getResponse();
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(httpStatusCode);
        response.getOutputStream().println(this.toString());
    }
    
    @Override
    public String toString(){
        return getRoot().toString();
    }
    public Html getRoot(){
        Html html = new Html();
        String applicationName = Config.instance().getProperty("swf.application.name");
        if (!ObjectUtil.isVoid(applicationName)){
            Title title = new Title();
            title.setText(applicationName);
            html.addControl(title);
        }
        SWFLogger cat = Config.instance().getLogger(getClass().getName());
        Timer htmlCreation = cat.startTimer("html creation.");
        try {
            createHtml(html);
        }finally {
            htmlCreation.stop();
        }

        Timer htmlToString = cat.startTimer("html rendering.");
        try {
            return html;
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
        head.addControl(new Css("/resources/scripts/node_modules/bootstrap/dist/css/bootstrap.min.css"));
        head.addControl(new Css("/resources/scripts/node_modules/@fortawesome/fontawesome-free/css/all.min.css"));
        head.addControl(new Css("/resources/scripts/node_modules/tablesorter/dist/css/theme.bootstrap.min.css"));
        head.addControl(new Css("/resources/scripts/node_modules/bootstrap4-datetimepicker/build/css/bootstrap-datetimepicker.min.css"));

        head.addControl(new Script("/resources/scripts/node_modules/jquery/dist/jquery.min.js",false));
        head.addControl(new Script("/resources/scripts/node_modules/popper.js/dist/umd/popper.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/bootstrap/dist/js/bootstrap.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/tablesorter/dist/js/jquery.tablesorter.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/tablesorter/dist/js/jquery.tablesorter.widgets.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/bootstrap-ajax-typeahead/bootstrap-typeahead.js"));
        head.addControl(new Script("/resources/scripts/node_modules/moment/min/moment-with-locales.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/bootstrap4-datetimepicker/build/js/bootstrap-datetimepicker.min.js"));
        head.addControl(new Script("/resources/scripts/node_modules/lockr/lockr.min.js"));
        
        
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
        Registry.instance().callExtensions("after.create.head",getPath(),head); // Global.
    }
    public void addProgressiveWebAppLinks(Head head){
        URL r = getClass().getResource("/manifest.json");
        String path = "";
        if (r == null){
            r = getClass().getResource("/web_manifest/manifest.json");
            path = "/web_manifest";
        }
        
        if (r == null){
            return;
        }
        String start_url = "/";
        String theme_color = "#000000";
        try { 
            JSONObject manifest = (JSONObject)JSONValue.parse(new InputStreamReader(r.openStream()));
            start_url = (String)manifest.getOrDefault("start_url",start_url);
            theme_color = (String)manifest.getOrDefault("theme_color",theme_color)  ;
        }catch(Exception ex){
            //
        }
        
        
        Link link  = new HLink(String.format("%s/manifest.json",path));
        link.setProperty("rel","manifest");
        head.addControl(link);

        link  = new HLink(String.format("%s/manifest.png",path)); //192x192
        link.setProperty("rel","icon");
        link.setProperty("type", MimeType.IMAGE_PNG.toString());
        head.addControl(link);

        link  = new HLink(String.format("%s/manifest.png",path));
        link.setProperty("rel","apple-touch-icon");
        link.setProperty("type", MimeType.IMAGE_PNG.toString());
        head.addControl(link);


        head.addControl(new Meta("mobile-web-app-capable" , "yes"));
        head.addControl(new Meta("apple-mobile-web-app-capable" , "yes"));
        head.addControl(new Meta( "theme-color",theme_color));
        String applicationName = getApplicationName();
        String applicationDescription = getApplicationDescription();

        head.addControl(new Meta("application-name" , applicationName));
        head.addControl(new Meta("apple-mobile-web-app-title" , applicationName));

        head.addControl(new Meta("msapplication-starturl",start_url));
        head.addControl(new Meta("viewport","width=device-width, initial-scale=1, shrink-to-fit=no"));
        head.addControl(new Meta("og:title",applicationName,true));
        head.addControl(new Meta("og:description",applicationDescription,true));
        Image logo = getLogo();
        if (logo != null && logo.getProperty("src") != null){
            head.addControl(new Meta("og:image",logo.getProperty("src"),true));
        }
        head.addControl(new Meta("og:url",Config.instance().getServerBaseUrl(),true));
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
    public void addHotLinks(_IControl b,  SequenceSet<HotLink> links, SequenceSet<HotLink> excludeLinks){
    	addHotLinks(b, b.getContainedControls().size(), links, excludeLinks);
    }
    public void addHotLinks(_IControl b, int index, SequenceSet<HotLink> links, SequenceSet<HotLink> excludeLinks){
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
