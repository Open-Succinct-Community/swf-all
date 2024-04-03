package com.venky.swf.db.model.application;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.api.EventHandler;

import java.util.List;

public interface Event extends Model {

    @UNIQUE_KEY
    @RegEx("[a-z,_]*")
    public String getName();
    public void setName(String name);

    public List<EventHandler> getEventHandlers();


    public static Event find(String name){
        Event event = Database.getTable(Event.class).newRecord();
        event.setName(name);
        event = Database.getTable(Event.class).find(event,false);
        return event;

    }
    public static class EventResult {
        public EventResult(EventHandler handler, Object response ,Object error){
            this.handler = handler;
            this.response = response;
            this.error = error;
        }
        public final EventHandler handler;
        public final Object response;
        public final Object error ;
    }
    public List<EventResult> raise(Object payload);
    public List<EventResult> raise(Application application, Object payload);
    public List<EventResult>  raise(List<EventHandler> handlers, Object payload);
}
