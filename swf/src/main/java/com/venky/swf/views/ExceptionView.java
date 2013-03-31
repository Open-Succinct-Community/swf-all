/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.text.Label;

/**
 *
 * @author venky
 */
public class ExceptionView extends HtmlView{
    Throwable th;
    public ExceptionView(_IPath path,Throwable th){
        super(path);
        this.th = th;
    }

    @Override
    protected void createBody(_IControl b) {
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        if (Config.instance().isDevelopmentEnvironment() || ObjectUtil.isVoid(th.getMessage())){
            th.printStackTrace(w);
        }else {
        	w.write(th.getMessage());
        }
    	
        Label lbl = new Label();
        lbl.setText(sw.toString());
        b.addControl(lbl);
        
    }
    
}
