package com.venky.swf.db.model.application;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.db.model.application.api.EventHandler;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class EventImpl extends ModelImpl<Event> {
    public EventImpl(Event event){
        super(event);
    }

    public void raise(Object payload){
        raise(getProxy().getEventHandlers(),payload);
    }
    public void raise(Application application, Object payload){
        ModelReflector<EventHandler> reflector = ModelReflector.instance(EventHandler.class);

        Select select = new Select().from(EventHandler.class).where(new Expression(reflector.getPool(), Conjunction.AND).
                add(new Expression(reflector.getPool(),"APPLICATION_ID", Operator.EQ,application.getId())).
                add(new Expression(reflector.getPool(),"EVENT_ID", Operator.EQ,getProxy().getId())));
        List<EventHandler> handlerList = select.execute();
        raise(handlerList,payload);
    }
    private void raise(List<EventHandler> handlers, Object payload){
        String sPayLoad = payload.toString();
        for (EventHandler eventHandler : handlers) {
            if (!eventHandler.isEnabled()){
                return;
            }
            
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
