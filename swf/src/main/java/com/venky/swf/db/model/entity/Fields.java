package com.venky.swf.db.model.entity;

import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;

public class Fields extends ObjectWrappers<Field> {
    public Fields(){
        this(new JSONArray());
    }
    protected Fields(JSONArray value) {
        super(value);
    }

    protected Fields(String payload) {
        super(payload);
    }
}
