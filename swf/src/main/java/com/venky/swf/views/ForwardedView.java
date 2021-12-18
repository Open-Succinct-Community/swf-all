/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;

import com.venky.swf.path.Path;
import jakarta.servlet.ServletException;

/**
 *
 * @author venky
 */
public class ForwardedView extends View{
	
    private String forwardToUrl;
    
    public String getForwaredToUrl() {
		return forwardToUrl;
	}

    public void setForwardedToUrl(String redirectUrl) {
		this.forwardToUrl = redirectUrl;
	}
	
    public ForwardedView(Path path,String controllerAction){
    	this(path,path.controllerPath(),controllerAction);
    }

    public ForwardedView(Path currentRequestPath, String redirectControllerPath, String redirectControllerAction){
        this(currentRequestPath);
        setForwardedToUrl(redirectControllerPath + "/" + redirectControllerAction);
    }
    
    public ForwardedView(Path path){
    	super(path);
    }
    
    
    public void write(int httpStatus) throws IOException {
        try {
            if (getPath().getAsyncContext() == null){
                getPath().getRequest().getRequestDispatcher(forwardToUrl).forward(getPath().getRequest(), getPath().getResponse());
            }else {
                getPath().getAsyncContext().dispatch(forwardToUrl);
            }
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
    }

}
