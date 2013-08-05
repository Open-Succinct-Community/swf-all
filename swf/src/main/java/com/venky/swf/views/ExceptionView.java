/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Response;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.text.Label;

/**
 *
 * @author venky
 */
public class ExceptionView extends View{
    final Throwable th;
    public ExceptionView(_IPath path,Throwable th){
        super(path);
        this.th = th;
    }

	@Override
	public void write() throws IOException {
        final StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        if (Config.instance().isDevelopmentEnvironment() || ObjectUtil.isVoid(th.getMessage())){
            th.printStackTrace(w);
        }else {
        	w.write(th.getMessage());
        }
        final Path p = (Path) getPath();
    	if (p.getProtocol() == MimeType.TEXT_HTML){
    		new HtmlView(p) {
				@Override
				protected void createBody(_IControl b) {
		            Label lbl = new Label();
		            lbl.setText(sw.toString());
		            b.addControl(lbl);
				}
			}.write();
    	}else {
    		IntegrationAdaptor<Response, ?> responseAdaptor = IntegrationAdaptor.instance(Response.class, FormatHelper.getFormatClass(p.getProtocol()));
    		responseAdaptor.createStatusResponse((Path) getPath(), th).write();
    	}
		
	}
    
}
