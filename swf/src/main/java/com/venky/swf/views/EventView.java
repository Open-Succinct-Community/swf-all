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

    public void write(int httpStatusCode){
        HttpServletResponse response = getPath().getResponse();
        response.setStatus(httpStatusCode);
        response.setContentType(MimeType.TEXT_EVENT_STREAM.toString());
        response.addHeader("Cache-Control","no-store");
        response.addHeader("Connection","keep-alive");
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        try {
            response.flushBuffer();
        }catch (Exception ex){
            //
        }
    }
    public void write(String event) throws IOException{
        HttpServletResponse response = getPath().getResponse();
        String wireEvent  = String.format("data: %s\n\n", event);

        response.getWriter().print(wireEvent);
        response.getWriter().flush();
        response.flushBuffer();
        Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Server Sent Event\n" + wireEvent);
    }

}
