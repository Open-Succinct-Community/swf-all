/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.Serializable;

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
