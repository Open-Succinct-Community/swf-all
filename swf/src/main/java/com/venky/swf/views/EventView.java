package com.venky.swf.views;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class EventView extends View {

    public EventView(Path path){
        super(path);
    }

    private boolean headersAdded = false;
    private void addHeaders() throws IOException{
        if (!headersAdded){
            headersAdded = true;
            Response response = getPath().getResponse();
            Mutable headers = response.getHeaders();
            response.setStatus(HttpStatus.OK_200);
            headers.put(HttpHeader.CONTENT_TYPE,"%s; charset=%s".formatted(MimeType.TEXT_EVENT_STREAM.toString(),StandardCharsets.UTF_8.toString()));
            headers.put("Cache-Control","no-store");
            headers.put("Connection","keep-alive");
            headers.put("Transfer-Encoding","chunked");
            response.write(false, ByteBuffer.wrap("retry: 1000\n\n".getBytes(StandardCharsets.UTF_8)),getPath().getCallback());
        }
    }

    public void write(int httpStatusCode){
        try {
            addHeaders();
        }catch (Exception ex){
            //
        }
    }
    public void write(String event) throws IOException{
        Response response = getPath().getResponse();
        String wireEvent  = String.format("data: %s\n\n", event);
        ByteBuffer payload = ByteBuffer.wrap(wireEvent.getBytes(StandardCharsets.UTF_8));

        addHeaders();
        response.write(false,payload,getPath().getCallback());
        Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Server Sent Event\n" + wireEvent);
    }

    }
