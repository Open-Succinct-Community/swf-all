package com.venky.swf.db.model.application;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.db.model.application.api.EventHandler;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.InputFormat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EventImpl extends ModelImpl<Event> {
    public EventImpl(Event event){
        super(event);
    }

    public void raise(Object payload){
        String sPayLoad = payload.toString();

        for (EventHandler eventHandler : getProxy().getEventHandlers()) {
            EndPoint endPoint = eventHandler.getEndPoint();

            if (!ObjectUtil.isVoid(eventHandler.getRelativeUrl())){
                Map<String,String> headers = new IgnoreCaseMap<>();
                headers.put("content-type",eventHandler.getContentType());
                ApplicationUtil.addAuthorizationHeader(eventHandler,headers,sPayLoad);

                new Call<InputStream>().url(String.format("%s/%s",
                                eventHandler.getEndPoint().getBaseUrl(),
                                eventHandler.getRelativeUrl())).headers(headers).
                        inputFormat(InputFormat.INPUT_STREAM).
                        input(new ByteArrayInputStream(sPayLoad.getBytes(StandardCharsets.UTF_8))).hasErrors();

            }
        }







    }
}
