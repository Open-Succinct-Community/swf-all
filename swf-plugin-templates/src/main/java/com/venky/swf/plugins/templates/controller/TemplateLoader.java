package com.venky.swf.plugins.templates.controller;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
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

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
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

    default HtmlView html(String path){
        return html(path,
                Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().valueOf(getPath().getFormFields().getOrDefault("includeMenu","N")));
    }

    default HtmlView html(String path, boolean includeMenu){
        return html(path,includeMenu,null);
    }

    default HtmlView html(String path, boolean includeMenu, Map<String,Object> data){
        if (includeMenu){
            return dashboard(new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html",data));
        }else {
            return new TemplateView(getPath(),getTemplateDirectory() ,"/html/"+path+".html",data);
        }
    }
    default HtmlView htmlFragment(String path){
        return htmlFragment(path,null);
    }
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

    @RequireLogin(false)
    default View images (String imageName){
        return load("images/"+imageName, MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(imageName),true);
    }


    default View load(String s,String contentType) {
        return load(s,contentType,false);
    }
    default View load(String s,String contentType,boolean raw) {
        try {
            if (raw){
                FileInputStream inputStream = new FileInputStream(new File(getTemplateDirectory(),s));
                return new BytesView(getPath(), StringUtil.readBytes(inputStream),contentType);
            }else{
                return new BytesView(getPath(), TemplateEngine.getInstance(getTemplateDirectory()).publish("/"+s, new HashMap<>()).getBytes(),contentType);
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


}
