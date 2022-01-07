package com.venky.swf.controller;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.util.PegDownProcessor;
import com.venky.swf.util.TemplateProcessor;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.TemplateView;
import com.venky.swf.views.TemplateView.Dummy;
import com.venky.swf.views.View;
import com.venky.swf.views.controls._IControl;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Locale;

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
    default View index(){
        if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/index.html")) {
            return html();
        }else if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/markdown/index.md")){
            return markdown();
        }else {
            return new RedirectorView(getPath(), "dashboard");
        }
    }

    enum TemplateSubDirectory {
        HTML(){
            public String contentType(String fileName){
                return MimeType.TEXT_HTML.toString();
            }

        },
        MARKDOWN(){
            @Override
            public String contentType(String fileName){
                return MimeType.TEXT_MARKDOWN.toString();
            }
            @Override
            public String fileExtension() {
                return ".md";
            }
        },
        JAVASCRIPT(){
            @Override
            public String dir() {
                return "js";
            }
            public String contentType(String fileName){
                return MimeType.TEXT_JAVASCRIPT.toString();
            }
        },
        CSS(){
            public String contentType(String fileName){
                return MimeType.TEXT_CSS.toString();
            }
        },
        IMAGES;
        public String contentType(String fileName){
            return MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName);
        }
        public String dir(){
            return toString().toLowerCase();
        }
        public String fileExtension(){
            return String.format(".%s",dir());
        }
        public String index(){
            return String.format("%s/index%s",dir(),fileExtension());
        }

    }


    default View publish(TemplateSubDirectory subDirectory){
        return new RedirectorView(getPath(), subDirectory.index());
    }

    default View publish(TemplateSubDirectory subDirectory,String file){
        return publish(subDirectory,file,
                Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().
                        valueOf(getPath().getFormFields().getOrDefault("includeMenu","N")));
    }
    default View publish(TemplateSubDirectory subDirectory,String file, boolean includeMenu){
        return publish(subDirectory,file,includeMenu,false);
    }

    default View publish(TemplateSubDirectory subDirectory, String file, boolean includeMenu, boolean fragment){
        String templateName = String.format("/%s/%s%s",subDirectory.dir(),file,file.endsWith(subDirectory.fileExtension())?"":subDirectory.fileExtension());
        HtmlView ret = null;
        try {
            FileInputStream inputStream = new FileInputStream(new File(getTemplateDirectory(),templateName));
            ret = new HtmlView(getPath()) {
                @Override
                protected void createBody(_IControl b) {
                    String p = StringUtil.read(inputStream);
                    if (subDirectory == TemplateSubDirectory.MARKDOWN){
                        p = pegDownProcessor.markdownToHtml(p);
                    }
                    b.addControl(new Dummy(p));
                }
            };
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

        if (includeMenu && !fragment){
            ret = dashboard(ret);
        }
        return ret;
    }

    static final PegDownProcessor pegDownProcessor = new PegDownProcessor();

    @RequireLogin(false)
    default View html(){
        return publish(TemplateSubDirectory.HTML);
    }
    @RequireLogin(false)
    default View html(String path){
        return publish(TemplateSubDirectory.HTML,path);
    }
    default View html(String path, boolean includeMenu){
        return publish(TemplateSubDirectory.HTML,path,includeMenu);
    }

    @RequireLogin(false)
    default View markdown(){
        return publish(TemplateSubDirectory.MARKDOWN);
    }
    @RequireLogin(false)
    default View markdown(String path){
        return publish(TemplateSubDirectory.MARKDOWN,path);
    }
    default View markdown(String path, boolean includeMenu){
        return publish(TemplateSubDirectory.MARKDOWN,path,includeMenu);
    }


    @RequireLogin(false)
    default View markdownFragment(String file){
        return publish(TemplateSubDirectory.MARKDOWN,file,false,true);
    }

    default View htmlFragment(String file){
        return publish(TemplateSubDirectory.HTML,file,false,true);
    }

    DashboardView dashboard(HtmlView aContainedView);

    @RequireLogin(false)
    default View js(String jsName){
        return load(TemplateSubDirectory.JAVASCRIPT,jsName);
        //return load("js"+"/" +jsName, "text/javascript");
    }

    @RequireLogin(false)
    default View css(String cssName){
        return load(TemplateSubDirectory.CSS,cssName);
        //return load("css"+"/" +cssName, "text/css");
    }


    @RequireLogin(false)
    default View images (String imageName){
        return load(TemplateSubDirectory.IMAGES,imageName);
        //return load("images/"+imageName, MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(imageName),true);
    }
    default View load(TemplateSubDirectory dir, String file) {
        return load(dir.dir()+"/"+file,dir.contentType(file));
    }

    default View load(String s,String contentType) {
        return load(s,contentType,false);
    }
    default View load(String s,String contentType,boolean raw) {
        try {
            FileInputStream inputStream = new FileInputStream(new File(getTemplateDirectory(),s));
            if (raw){
                return new BytesView(getPath(), StringUtil.readBytes(inputStream),contentType,"content-disposition", "attachment; filename=" + s);
            }else{
                return new BytesView(getPath(), StringUtil.readBytes(inputStream),contentType);
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }


}
