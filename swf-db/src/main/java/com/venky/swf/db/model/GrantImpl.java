package com.venky.swf.db.model;

import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class GrantImpl extends ModelImpl<Grant> {
    public GrantImpl(Grant grant){
        super(grant);
    }
    public String getTokenType(){
        return "Bearer";
    }

    public String getIdToken() {
        JSONObject header = new JSONObject();
        JSONObject body = new JSONObject();
        //body.put("email", getProxy().getUser().getEmail());
        body.put("name", getProxy().getUser().getName());
        body.put("aud", getProxy().getApplication().getAppId());
        body.put("iss", Config.instance().getServerBaseUrl());
        return Base64.getEncoder().encodeToString(header.toString().getBytes(StandardCharsets.UTF_8)) + "." +
                Base64.getEncoder().encodeToString(body.toString().getBytes(StandardCharsets.UTF_8)) + ".";
    }
}
