package com.venky.swf.db.model.application.api;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class OpenApiImpl extends ModelImpl<OpenApi> {
    public OpenApiImpl() {
    }

    public OpenApiImpl(OpenApi proxy) {
        super(proxy);
    }
    public String getSpecificationUrl(){
        String location = getProxy().getSpecificationLocation();
        if (location == null){
            return null;
        }
        if (location.startsWith("/") ){
            return String.format("%s%s",Config.instance().getServerBaseUrl(),location);
        }else {
            return location;
        }
    }
}
