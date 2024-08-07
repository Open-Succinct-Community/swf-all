package com.venky.swf.views;

import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class EventView extends View {

    public EventView(Path path){
        super(path);
    }

    private boolean headersAdded = false;
    private void addHeaders(){
        if (!headersAdded){
            headersAdded = true;
            HttpServletResponse response = getPath().getResponse();
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
            response.setContentType(MimeType.TEXT_EVENT_STREAM.toString());
            response.setHeader("Cache-Control","no-store");
            response.setHeader("Connection","keep-alive");
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        }
    }

    public void write(int httpStatusCode){
        try {
            addHeaders();
            getPath().getResponse().flushBuffer();
        }catch (Exception ex){
            //
        }
    }
    public void write(String event) throws IOException{
        HttpServletResponse response = getPath().getResponse();
        String wireEvent  = String.format("data: %s\n\n", event);

        addHeaders();
        response.getWriter().print(wireEvent);
        response.getWriter().flush();

        response.flushBuffer();
        Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Server Sent Event\n" + wireEvent);
    }

    }
