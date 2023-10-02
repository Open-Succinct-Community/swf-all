package com.venky.swf.db.model.entity;

import in.succinct.json.JSONObjectWrapper;
import in.succinct.json.ObjectWrappers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ChildEntity extends JSONObjectWrapper {
    public ChildEntity(){
        this(new JSONObject());
    }
    protected ChildEntity(JSONObject value) {
        super(value);
    }

    protected ChildEntity(String payload) {
        super(payload);
    }

    public boolean isVirtual(){
        return getBoolean("Virtual");
    }
    public void setVirtual(boolean virtual){
        set("Virtual",virtual);
    }
    public String getName(){
        return get("Name");
    }
    public void setName(String name){
        set("Name",name);
    }


    public Fields getReferenceFields(){
        return get(Fields.class, "ReferenceFields");
    }
    public void setReferenceFields(Fields reference_field){
        set("ReferenceFields",reference_field);
    }
    public static class ChildEntities extends ObjectWrappers<ChildEntity>{
        public ChildEntities(){
            this(new JSONArray());
        }
        protected ChildEntities(JSONArray value) {
            super(value);
        }

        protected ChildEntities(String payload) {
            super(payload);
        }
    }
}
