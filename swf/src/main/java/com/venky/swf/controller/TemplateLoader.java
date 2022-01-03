package com.venky.swf.controller;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.util.PegDownProcessor;
import com.venky.swf.util.TemplateProcessor;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.TemplateView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.layout.headings.H;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public interface TemplateLoader {

    public Path getPath();
    default String getTemplateDirectory() {
        int upto = getClass().getSimpleName().lastIndexOf("Controller");
        String subdirectory = "";
        if (upto > 0 ) {
            subdirectory = LowerCaseStringCache.instance().get(getClass().getSimpleName().substring(0, upto));
        }

        return getTemplateDirectory(subdirectory);
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
    default View html(){
        return new RedirectorView(getPath(),"html/index");
    }

    @RequireLogin(false)
    default View html(String path){
        return html(path,
                Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().valueOf(getPath().getFormFields().getOrDefault("includeMenu","N")));
    }

    @RequireLogin(false)
    default View index(){
        if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/index.html")) {
            return html();
        }else if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/markdown/index.md")){
            return markdown();
        }else {
            return new RedirectorView(getPath(), "dashboard");
        }
    }

    @RequireLogin(false)
    default View markdown(){
        return new RedirectorView(getPath(),"markdown/index");
    }
    @RequireLogin(false)
    default View markdown(String path){
        return markdown(path,
                Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().valueOf(getPath().getFormFields().getOrDefault("includeMenu","N")));
    }

    default View html(String path, boolean includeMenu){
        return html(path,includeMenu,null);
    }
    default View markdown(String path, boolean includeMenu){
        return markdown(path,includeMenu,null);
    }

    default View html(String path, boolean includeMenu, Map<String,Object> data){
        View ret =  template("/html/"+path+".html",data);
        if (includeMenu && ret instanceof HtmlView){
            ret = dashboard((HtmlView) ret);
        }
        return ret;
    }

    default View markdown(String path, boolean includeMenu, Map<String,Object> data){
        View ret = template("/markdown/"+path+".md",data);
        if (includeMenu && ret instanceof HtmlView){
            ret = dashboard((HtmlView) ret);
        }
        return ret;
    }

    @RequireLogin(false)
    default View markdownFragment(String path){
        return markdownFragment(path,null);
    }
    default View markdownFragment(String path, Map<String,Object> data){
        return template("/markdown/"+path+".md",data,true);
    }

    default View template(String templateName, Map<String,Object> data ){
        return template(templateName,data,false);
    }

    static final PegDownProcessor pegDownProcessor = new PegDownProcessor();
    default View template(String templateName, Map<String, Object> data, boolean fragment) {
        return new TemplateView(getPath(),getTemplateDirectory() ,templateName,data,fragment){
            @Override
            protected String publish() {
                String p = super.publish();
                if (templateName.endsWith(".md")){
                    p = pegDownProcessor.markdownToHtml(p);
                }
                return p;
            }
        };
    }

    default View htmlFragment(String path){
        return htmlFragment(path,null);
    }
    default View htmlFragment(String path, Map<String,Object> data){
        return template("/html/"+path+".html",data,true);
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
                return new BytesView(getPath(), StringUtil.readBytes(inputStream),contentType,"content-disposition", "attachment; filename=" + s);
            }else{
                return new BytesView(getPath(), TemplateProcessor.getInstance(getTemplateDirectory()).publish("/"+s, new HashMap<>()).getBytes(),contentType);
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


}
