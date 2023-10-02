package com.venky.swf.db.model.entity;

import com.esotericsoftware.kryo.util.IntMap.Values;
import com.venky.core.string.StringUtil;
import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONObject;


public class Field extends JSONAwareWrapper<JSONObject> {
    public Field(){
        this(new JSONObject());
    }
    protected Field(JSONObject value) {
        super(value);
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

    public boolean isNullable(){
        return getBoolean("Nullable");
    }
    public void setNullable(boolean nullable){
        set("Nullable",nullable);
    }

    public String getJavaClass(){
        return get("JavaClass");
    }
    public void setJavaClass(String java_class){
        set("JavaClass",java_class);
    }

    public String getReferenceEntityName(){
        return get("ReferenceEntityName");
    }
    public void setReferenceEntityName(String reference_entity_name){
        set("ReferenceEntityName",reference_entity_name);
    }

    public Strings getValues(){
        return get(Strings.class, "Values");
    }
    public void setValues(Strings values){
        set("Values",values);
    }

}
