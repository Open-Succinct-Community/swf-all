package com.venky.swf.db.model.entity;

import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;

public class Actions extends ObjectWrappers<Action> {
    public Actions(){
        this(new JSONArray());
    }
    protected Actions(JSONArray value) {
        super(value);
    }

    protected Actions(String payload) {
        super(payload);
    }
}
