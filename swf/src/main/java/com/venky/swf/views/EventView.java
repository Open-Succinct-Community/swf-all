package com.venky.swf.views;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class EventView extends View {

    public EventView(_IPath path){
        super(path);
    }

    private boolean headersAdded = false;
    private boolean beingForwarded = true;
    private void addHeaders(int status) throws IOException{
        if (!headersAdded){
            headersAdded = true;
            Response response = getPath().getResponse();
            Mutable headers = response.getHeaders();
            response.setStatus(status);
            headers.put(HttpHeader.CONTENT_TYPE,"%s; charset=%s".formatted(MimeType.TEXT_EVENT_STREAM.toString(),StandardCharsets.UTF_8.toString()));
            headers.put("Cache-Control","no-store");
            headers.put("Connection","keep-alive");
            headers.put("Transfer-Encoding","chunked");
            Content.Sink.write(response,false, "retry: 1000\n\n", Callback.NOOP);
        }
    }

    public void write(int httpStatusCode){
        try {
            addHeaders(httpStatusCode);
        }catch (Exception ex){
            //
        }
    }
    public void write(String event, boolean isLastEvent) throws IOException{
        Response response = getPath().getResponse();
        String wireEvent  = String.format("data: %s\n\n", event);
        
        addHeaders(HttpStatus.OK_200);
        Content.Sink.write(response,false,wireEvent,Callback.NOOP); //Mention last always as false as we close explicitly based on this.last;
        Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Server Sent Event\n" + wireEvent);

        this.beingForwarded = !isLastEvent;
    }
    
    @Override
    public boolean isBeingForwarded() {
        return beingForwarded;
    }
}
