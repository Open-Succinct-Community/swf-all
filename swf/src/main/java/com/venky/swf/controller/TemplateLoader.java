package com.venky.swf.controller;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
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
import com.venky.swf.views.ForwardedView;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.RedirectorView;
import com.venky.swf.views.View;
import com.venky.swf.views.controls._IControl;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

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
        File checkOverride= new File(dir.toString(),Config.instance().getHostName());
        if ( checkOverride.exists() && checkOverride.isDirectory()) {
            dir.append("/").append(Config.instance().getHostName());
        }
        dir.append("/").append(subdirectory);


        return dir.toString();
    }

    @RequireLogin(false)
    default View index(){
        if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/app/index.html")) {
            return app();
        }else if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/html/index.html")) {
            return html();
        }else if (TemplateProcessor.getInstance(getTemplateDirectory()).exists("/markdown/index.md")){
            return markdown();
        }else {
            return new ForwardedView(getPath(), "dashboard");
        }
    }

    @RequireLogin(false)
    default View app(){
        return new RedirectorView(getPath(),"app/index");
    }
    @RequireLogin(false)
    default View app(String path)  {
        return publish(TemplateSubDirectory.APP,path,false,true);
    }
    enum TemplateSubDirectory {
        HTML(){
            public String contentType(String fileName){
                return MimeType.TEXT_HTML.toString();
            }

        },
        APP(){
            @Override
            public String fileExtension() {
                return ".html";
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
        IMAGES,
        FONTS;
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
        return new ForwardedView(getPath(), subDirectory.index());
    }

    default View publish(TemplateSubDirectory subDirectory,String file){
        return publish(subDirectory,file,isMenuIncluded());
    }
    default View publish(TemplateSubDirectory subDirectory,String file, boolean includeMenu){
        return publish(subDirectory,file,includeMenu,false);
    }

    default View publish(TemplateSubDirectory subDirectory, String file, boolean includeMenu, boolean fragment){
        SequenceSet<String> filesToCheck = new SequenceSet<>();
        // dir/file or dir/file.html
        if (file.lastIndexOf(".") > file.lastIndexOf("/")) {
            filesToCheck.add(String.format("/%s/%s",subDirectory.dir(),file));
        }else {
            filesToCheck.add(String.format("/%s/%s%s", subDirectory.dir(), file, subDirectory.fileExtension()));
        }
        filesToCheck.add(String.format("/%s", subDirectory.index()));

        File template = null;
        boolean treatAsFragment = fragment || (subDirectory == TemplateSubDirectory.APP);
        for (String templateName : filesToCheck){
            template = new File(getTemplateDirectory(),templateName);
            if (template.exists() && template.isFile()){
                if (!templateName.endsWith(subDirectory.fileExtension()) && !treatAsFragment){
                    treatAsFragment = true;
                }
                break;
            }
        }

        View ret = null;
        try {
            FileInputStream inputStream = new FileInputStream(template);
            byte [] bytes = StringUtil.readBytes(inputStream);

            if (subDirectory == TemplateSubDirectory.MARKDOWN){
                bytes = pegDownProcessor.markdownToHtml(new String(bytes)).getBytes(StandardCharsets.UTF_8);
            }
            if (!treatAsFragment) {
                final String processed = new String(bytes);
                ret = new HtmlView(getPath()) {
                    @Override
                    protected void createBody(_IControl b) {
                        b.addControl(new Dummy(processed));
                    }

                    @Override
                    public String toString() {
                        return super.toString();
                    }
                };
                if (includeMenu){
                    ret = dashboard((HtmlView) ret);
                }
            }else {
                ret = new BytesView(getPath(), bytes, MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(template.getName()));
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

        return ret;
    }

    static final PegDownProcessor pegDownProcessor = new PegDownProcessor();

    @RequireLogin(false)
    default View html(){
        return html(new File(TemplateSubDirectory.HTML.index()).getName());
    }
    @RequireLogin(false)
    default View html(String path){
        return html(path,isMenuIncluded());
    }
    default boolean isMenuIncluded(){
        return Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter().
                valueOf(getPath().getFormFields().getOrDefault("includeMenu","N"));
    }
    default View html(String path, boolean includeMenu){
        return publish(TemplateSubDirectory.HTML,path,includeMenu);
    }

    @RequireLogin(false)
    default View markdown(){
        return markdown(new File(TemplateSubDirectory.MARKDOWN.index()).getName());
    }
    @RequireLogin(false)
    default View markdown(String path){
        return markdown(path,false);
    }
    default View markdown(String path, boolean includeMenu){
        return publish(TemplateSubDirectory.MARKDOWN,path,includeMenu);
    }


    @RequireLogin(false)
    default View markdownFragment(String file){
        return publish(TemplateSubDirectory.MARKDOWN,file,false);
    }

    @RequireLogin(false)
    default View htmlFragment(String file){
        return publish(TemplateSubDirectory.HTML,file,false);
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

    @RequireLogin(false)
    default View fonts (String file){
        return load(TemplateSubDirectory.FONTS,file);
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
