/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;

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
		this.redirectUrl = redirectUrl;
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
        HttpServletResponse response = getPath().getResponse();
        response.setContentType("text/plain");
        if (!redirectUrl.startsWith("/") && !redirectUrl.startsWith("http")){
            redirectUrl = "/" + redirectUrl;
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public boolean isBeingRedirected(){
    	return true;
    }
}
