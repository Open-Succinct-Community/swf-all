/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.text.Label;

/**
 *
 * @author venky
 */
public class ExceptionView extends HtmlView{
    Throwable th;
    public ExceptionView(_IPath path,Throwable th){
        super(path);
        this.th = ExceptionUtil.getRootCause(th);
    }

    @Override
    protected void createBody(Body b) {
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        if (Config.instance().isDevelopmentEnvironment()){
            th.printStackTrace(w);
        }else {
        	w.write(th.toString());
        }
    	
        Label lbl = new Label();
        lbl.setText(sw.toString());
        b.addControl(lbl);
        
    }
    
}
