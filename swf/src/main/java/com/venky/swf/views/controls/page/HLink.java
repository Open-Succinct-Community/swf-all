/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

/**
 *
 * @author venky
 */
public class HLink extends Link  {

	public HLink(){
        this(null);
    }
    public HLink(String url){
        this("link",url);
    }
    protected HLink(String tag, String url){
        super(tag,url);
    }
}
