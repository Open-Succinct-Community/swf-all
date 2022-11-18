package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.api.EventHandler;

import java.util.List;

public interface Event extends Model {

    @UNIQUE_KEY
    @RegEx("[a-z]*")
    public String getName();
    public void setName(String name);

    public List<EventHandler> getEventHandlers();


    public static Event find(String name){
        Event event = Database.getTable(Event.class).newRecord();
        event.setName(name);
        event = Database.getTable(Event.class).find(event,false);
        return event;

    }
    public void raise(Object payload);
}
