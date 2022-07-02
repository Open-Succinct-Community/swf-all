package com.venky.swf.controller;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;

public class PropertiesController extends Controller {
    public PropertiesController(Path path) {
        super(path);
    }
    public View get(String key){
        JSONObject ret = new JSONObject();
        Registry.instance().callExtensions("secure.property",key);
        ret.put("key",key);
        ret.put("value",Config.instance().getProperty(key));
        return new BytesView(getPath(),ret.toString().getBytes());
    }

    @RequireLogin(false)
    public View environment(){
        return get("swf.env");
    }

    public View put(String envString){
        String[] k = envString.split("=");
        if (k != null && k.length == 2) {
            Config.instance().setProperty(k[0], k[1]);
            return get(k[0]);
        }
        throw new RuntimeException("Invalid parameters");
    }

    public View remove(String key){
        Config.instance().removeProperty(key);
        return new BytesView(getPath(),"Property removed".getBytes());
    }
}
