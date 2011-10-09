/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.text.Label;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author venky
 */
public class ExceptionView extends HtmlView{
    Throwable th;
    public ExceptionView(Path path,Throwable th){
        super(path);
        this.th = ExceptionUtil.getRootCause(th);
    }

    @Override
    protected void createBody(Body b) {
    	/*
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        th.printStackTrace(w);
		*/
    	
        Label lbl = new Label();
        lbl.setText(th.getMessage());
        b.addControl(lbl);
        
    }
    
}
