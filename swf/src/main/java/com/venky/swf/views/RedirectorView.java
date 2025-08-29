/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author venky
 */
public class RedirectorView extends View{
	
    private String redirectUrl;
    
    public String getRedirectUrl() {
		return redirectUrl;
	}

    public void setRedirectUrl(String redirectUrl) {

        Pattern pattern = Pattern.compile("^(http[s]?):(/.*)");
        String x = redirectUrl.replaceAll("[/]+","/");
        Matcher m = pattern.matcher(x);
        if (m.find()){
            this.redirectUrl = m.group(1) + ":/" + m.group(2);
        }else {
            this.redirectUrl = redirectUrl;
        }

	}
	
    public RedirectorView(_IPath path,String controllerAction){
    	this(path,path.controllerPath(),controllerAction);
    }

    public RedirectorView(_IPath currentRequestPath, String redirectControllerPath, String redirectControllerAction){
        this(currentRequestPath);
        if (!ObjectUtil.isVoid(redirectControllerAction)){
            setRedirectUrl(redirectControllerPath + "/" + redirectControllerAction);
        }else{
            setRedirectUrl(redirectControllerPath);
        }
    }
    
    public RedirectorView(_IPath path){
    	super(path);
        setRedirectUrl(path.getTarget());
    }
    

    
    
    public void write(int httpStatusCode) throws IOException {
        Response response = getPath().getResponse();
        response.getHeaders().put(HttpHeader.CONTENT_TYPE,"text/plain");
        if (!redirectUrl.startsWith("/") && !redirectUrl.startsWith("http")){
            redirectUrl = "/" + redirectUrl;
        }
        if (redirectUrl.startsWith("/")){
            redirectUrl= Config.instance().getServerBaseUrl() + redirectUrl;
        }
        response.getHeaders().put(HttpHeader.LOCATION,redirectUrl);
        response.setStatus(HttpStatus.MOVED_TEMPORARILY_302);
        response.write(false, ByteBuffer.wrap(new byte[]{}), Callback.NOOP);
    }

    @Override
    public boolean isBeingRedirected(){
    	return true;
    }
}
