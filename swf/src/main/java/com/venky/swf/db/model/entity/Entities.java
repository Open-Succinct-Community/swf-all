package com.venky.swf.db.model.entity;

import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;

public class Entities extends ObjectWrappers<Entity> {
    public Entities(){
        this(new JSONArray());
    }
    protected Entities(JSONArray value) {
        super(value);
    }

    protected Entities(String payload) {
        super(payload);
    }

}
