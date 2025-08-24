/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.HttpTask;
import com.venky.swf.routing.Router;

import java.io.IOException;


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
            Router.instance().getTaskManager().submit(new HttpTask(getPath().constructNewPath(getForwaredToUrl())));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public boolean isBeingForwarded() {
        return true;
    }
}
