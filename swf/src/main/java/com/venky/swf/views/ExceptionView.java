/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.SWFHttpResponse;
import com.venky.swf.db.table.RecordNotFoundException;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.exceptions.UserNotAuthenticatedException;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.text.Label;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author venky
 */
public class ExceptionView extends View{
    final Throwable th;
    public ExceptionView(_IPath path,Throwable th){
        super(path);
        this.th = ExceptionUtil.getRootCause(th);
    }
    public void write() throws IOException{
        int httpStatus = HttpStatus.BAD_REQUEST_400;
        if (ExceptionUtil.getEmbeddedException(th,AccessDeniedException.class) instanceof AccessDeniedException ) {
            httpStatus = HttpStatus.FORBIDDEN_403;
        }else if (ExceptionUtil.getEmbeddedException(th,UserNotAuthenticatedException.class) instanceof UserNotAuthenticatedException){
            httpStatus = HttpStatus.UNAUTHORIZED_401;
        }else if (ExceptionUtil.getEmbeddedException(th, RecordNotFoundException.class) instanceof RecordNotFoundException) {
            httpStatus = HttpStatus.NOT_FOUND_404;
        }
        write(httpStatus);
    }

	public void write(int httpStatus) throws IOException {
        final StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        if (Config.instance().isDevelopmentEnvironment() || ObjectUtil.isVoid(th.getMessage())){
            th.printStackTrace(w);
        }else {
        	w.write(th.getMessage());
        }
        final Path p = (Path) getPath();
    	if (p.getReturnProtocol() == MimeType.TEXT_HTML){
    		new HtmlView(p) {
				@Override
				protected void createBody(_IControl b) {
		            Label lbl = new Label();
		            lbl.setText(sw.toString());
		            b.addControl(lbl);
				}
			}.write(httpStatus);
    	}else {
    		IntegrationAdaptor<SWFHttpResponse, ?> responseAdaptor = IntegrationAdaptor.instance(SWFHttpResponse.class, FormatHelper.getFormatClass(p.getReturnProtocol()));
    		responseAdaptor.createStatusResponse((Path) getPath(), th).write(httpStatus);
    	}
		
	}
    
}
