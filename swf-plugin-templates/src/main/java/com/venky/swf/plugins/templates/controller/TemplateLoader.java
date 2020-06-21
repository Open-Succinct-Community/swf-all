package com.venky.swf.plugins.templates.controller;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.plugins.templates.views.TemplateView;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;

import java.util.HashMap;
import java.util.Map;

public interface TemplateLoader {

    public Path getPath();
    default String getTemplateDirectory() {
        return Config.instance().getProperty("swf.ftl.dir");
    }

    default String getTemplateDirectory(String subdirectory){
        StringBuilder dir = new StringBuilder();
        String templateDirectory  = Config.instance().getProperty("swf.ftl.dir");
        if (!ObjectUtil.isVoid(templateDirectory)){
            dir.append(templateDirectory);
        }
        dir.append("/").append(subdirectory);
        return dir.toString();
    }

    @RequireLogin(false)
    default HtmlView html(String path){
        return html(path,true);
    }
    @RequireLogin(false)
    default HtmlView html(String path, boolean includeMenu){
        return html(path,includeMenu,null);
    }
    @RequireLogin(false)
    default HtmlView html(String path, boolean includeMenu, Map<String,Object> data){
        if (includeMenu){
            return dashboard(new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html",data));
        }else {
            return new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html",data);
        }
    }
    @RequireLogin(false)
    default HtmlView htmlFragment(String path, Map<String,Object> data){
        return new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html",data,true);
    }

    DashboardView dashboard(HtmlView aContainedView);

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
