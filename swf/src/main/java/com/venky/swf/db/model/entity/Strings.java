package com.venky.swf.db.model.entity;

import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;

public class Strings extends ObjectWrappers<String> {
    public Strings(){
        this(new JSONArray());
    }
    protected Strings(JSONArray value) {
        super(value);
    }

    protected Strings(String payload) {
        super(payload);
    }
}
