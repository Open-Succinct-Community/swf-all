/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;

/**
 *
 * @author venky
 */
public class BytesView extends View{
    private byte[] bytes;
    private String contentType;

    public String getContentType() {
        return contentType;
    }

    Map<String,String> addnlResponseHeaders = new HashMap<String, String>();
    public BytesView(Path path,byte[] bytes){
        this(path,bytes,MimeType.TEXT_PLAIN);
    }
    public BytesView(Path path,byte[] bytes,MimeType contentType,String... responseHeaderAttributes){
    	this(path,bytes,contentType.toString(),responseHeaderAttributes);
    }
    public BytesView(Path path,byte[] bytes,String contentType,String... responseHeaderAttributes){
    	super(path);
        this.bytes = bytes;
        this.contentType = MimeType.TEXT_PLAIN.toString(); 
        if (contentType != null){ 
        	this.contentType = contentType;
        }
        if ( responseHeaderAttributes != null && responseHeaderAttributes.length % 2 == 0 ){
        	for (int i = 0 ; i < responseHeaderAttributes.length ; i ++ ) {
        		addnlResponseHeaders.put(responseHeaderAttributes[i], responseHeaderAttributes[i+1]);
        		i++;
        	}
        }
    }
    public void write(int httpStatusCode) throws IOException {
        HttpServletResponse response = getPath().getResponse();
        response.setContentType(contentType);
        if (addnlResponseHeaders != null){
        	for (String key:addnlResponseHeaders.keySet()){
        		response.addHeader(key, addnlResponseHeaders.get(key));
        	}
        }
        response.setStatus(httpStatusCode);
        //response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
