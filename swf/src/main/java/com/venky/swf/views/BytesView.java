/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author venky
 */
public class BytesView extends View{
    private byte[] bytes;
    private MimeType contentType ; 
    public BytesView(Path path,byte[] bytes){
        this(path,bytes,null);
    }
    public BytesView(Path path,byte[] bytes,MimeType contentType){
        super(path);
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public void write() throws IOException {
        HttpServletResponse response = getPath().getResponse();
        if (contentType != null ) {
            response.setContentType(contentType.toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(bytes);

    }
}
