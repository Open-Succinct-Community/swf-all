/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

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

    public byte[] getBytes() {
        return bytes;
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
        Response response = getPath().getResponse();
        response.getHeaders().put("Content-Type",contentType);
        if (addnlResponseHeaders != null){
        	for (String key:addnlResponseHeaders.keySet()){
        		response.getHeaders().add(key, addnlResponseHeaders.get(key));
        	}
        }
        response.setStatus(httpStatusCode);
        //response.setContentLength(bytes.length);
        response.write(false,ByteBuffer.wrap(bytes), Callback.NOOP);
    }
}
