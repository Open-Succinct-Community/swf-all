package com.venky.swf.db.model.entity;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;
import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONObject;


public class Action extends JSONAwareWrapper<JSONObject> {
    public Action(){
        super(new JSONObject());
    }

    public String getName(){
        return get("Name");
    }
    public void setName(String name){
        set("Name",name);
    }

    public boolean isRequireLogin(){
        return getBoolean("RequireLogin");
    }
    public void setRequireLogin(boolean require_login){
        set("RequireLogin",require_login);
    }


    public String getIcon(){
        return get("Icon");
    }
    public void setIcon(String icon){
        set("Icon",icon);
    }
    
    public String getToolTip(){
        return get("ToolTip");
    }
    public void setToolTip(String tool_tip){
        set("ToolTip",tool_tip);
    }

    public boolean isAllowed(){
        return getBoolean("Allowed");
    }
    public void setAllowed(boolean allowed){
        set("Allowed",allowed);
    }

    public boolean isSingleRecordAction(){
        return getBoolean("SingleRecordAction");
    }
    public void setSingleRecordAction(boolean single_record_action){
        set("SingleRecordAction",single_record_action);
    }


    public String getHttpMethod(){
        return get("HttpMethod");
    }
    public void setHttpMethod(String http_method){
        set("HttpMethod",http_method);
    }




}
