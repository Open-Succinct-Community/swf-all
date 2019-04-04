package com.venky.swf.controller;

import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import org.json.simple.JSONObject;

public class PropertiesController extends Controller {
    public PropertiesController(Path path) {
        super(path);
    }
    public View get(String key){
        JSONObject ret = new JSONObject();
        ret.put("key",key);
        ret.put("value",Config.instance().getProperty(key));
        return new BytesView(getPath(),ret.toString().getBytes());
    }
}
