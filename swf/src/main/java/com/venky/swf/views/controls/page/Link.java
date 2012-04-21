/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class Link extends Control{
    /**
	 * 
	 */
	private static final long serialVersionUID = -4400380426867221125L;
	public  Link(){
        this(null);
    }
    public Link(String url){
        this("a",url);
    }
    protected Link(String tag,String url){
        super(tag);
        if (url != null){
            setUrl(url);
        }
    }
    public final void setUrl(String path){
        setProperty("href", path);
    }
    
    public final String getUrl(){
    	return getProperty("href");
    }
}
