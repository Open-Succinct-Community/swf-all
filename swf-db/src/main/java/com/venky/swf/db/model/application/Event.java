package com.venky.swf.db.model.application;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

import java.util.List;
import java.util.Map;

public interface Event extends Model {

    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    public List<ApplicationEvent> getApplicationEvents();


    public static Event find(String name){
        Event event = Database.getTable(Event.class).newRecord();
        event.setName(name);
        event = Database.getTable(Event.class).find(event,false);
        return event;

    }
    public void raise(Map<String,Object> variables);
}
