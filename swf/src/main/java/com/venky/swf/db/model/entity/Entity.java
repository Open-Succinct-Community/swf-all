package com.venky.swf.db.model.entity;

import com.venky.swf.db.model.entity.ChildEntity.ChildEntities;
import in.succinct.json.JSONObjectWrapper;
import org.json.simple.JSONObject;

public class Entity extends JSONObjectWrapper {

    public Entity(){
        this(new JSONObject());
    }
    protected Entity(JSONObject value) {
        super(value);
    }

    protected Entity(String payload) {
        super(payload);
    }

    public String getName(){
        return get("Name");
    }
    public void setName(String name){
        set("Name",name);
    }

    public boolean isVirtual(){
        return getBoolean("Virtual");
    }
    public void setVirtual(boolean virtual){
        set("Virtual",virtual);
    }

    public Fields getFields(){
        return get(Fields.class, "Fields");
    }
    public void setFields(Fields fields){
        set("Fields",fields);
    }

    public ChildEntities getChildren(){
        return get(ChildEntities.class, "Children");
    }
    public void setChildren(ChildEntities children){
        set("Children",children);
    }

    public Actions getActions(){
        return get(Actions.class, "Actions");
    }
    public void setActions(Actions actions){
        set("Actions",actions);
    }

}
