/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.venky.swf.routing.Path;

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
	
    public RedirectorView(Path path,String controllerAction){
    	this(path,path.controllerPath(),controllerAction);
    }

    public RedirectorView(Path currentRequestPath, String redirectControllerPath, String redirectControllerAction){
        this(currentRequestPath);
        setRedirectUrl(redirectControllerPath + "/" + redirectControllerAction);
    }
    
    public RedirectorView(Path path){
    	super(path);
    }
    

    
    
    @Override
    public void write() throws IOException {
        HttpServletResponse response = getPath().getResponse();
        response.setContentType("text/plain");
        response.sendRedirect(redirectUrl);
    }
    
}
