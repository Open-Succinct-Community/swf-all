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
    private MimeType contentType ; 
    Map<String,String> addnlResponseHeaders = new HashMap<String, String>();
    public BytesView(Path path,byte[] bytes){
        this(path,bytes,null);
    }
    public BytesView(Path path,byte[] bytes,MimeType contentType,String... responseHeaderAttributes){
    	super(path);
        this.bytes = bytes;
        this.contentType = contentType;
        if ( responseHeaderAttributes != null && responseHeaderAttributes.length % 2 == 0 ){
        	for (int i = 0 ; i < responseHeaderAttributes.length ; i ++ ) {
        		addnlResponseHeaders.put(responseHeaderAttributes[i], responseHeaderAttributes[i+1]);
        		i++;
        	}
        }
    }
    public void write() throws IOException {
        HttpServletResponse response = getPath().getResponse();
        if (contentType != null ) {
            response.setContentType(contentType.toString());
        }
        if (addnlResponseHeaders != null){
        	for (String key:addnlResponseHeaders.keySet()){
        		response.addHeader(key, addnlResponseHeaders.get(key));
        	}
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(bytes);

    }
}
