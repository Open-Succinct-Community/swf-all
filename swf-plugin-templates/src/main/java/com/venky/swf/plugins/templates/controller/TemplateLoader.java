package com.venky.swf.plugins.templates.controller;

import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.plugins.templates.views.TemplateView;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.HashMap;

public interface TemplateLoader {

    public Path getPath();
    public String getTemplateDirectory();

    @RequireLogin(false)
    default View html(String path){
        return new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html");
    }

    @RequireLogin(false)
    default View js(String jsName){
        return load("js"+"/" +jsName, "text/javascript");
    }
    @RequireLogin(false)
    default View css(String cssName){
        return load("css"+"/" +cssName, "text/css");
    }

    @RequireLogin(false)
    default View fragment    (String fragment){
        return load("fragment/"+fragment+".html","text/plain");
    }

    @RequireLogin
    default View images (String imageName){
        return load("images/"+imageName, MimeType.APPLICATION_OCTET_STREAM.toString());
    }


    default View load(String s,String contentType) {
        return new BytesView(getPath(), TemplateEngine.getInstance(getTemplateDirectory()).publish("/"+s, new HashMap<>()).getBytes(),contentType);
    }

}
