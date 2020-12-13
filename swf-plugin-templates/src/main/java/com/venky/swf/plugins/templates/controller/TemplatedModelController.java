package com.venky.swf.plugins.templates.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.View;

public class TemplatedModelController<M extends Model> extends ModelController<M> implements TemplateLoader {
    public TemplatedModelController(Path path) {
        super(path);
    }

    @Override
    public String getTemplateDirectory() {
        return getTemplateDirectory(getReflector().getTableName().toLowerCase());
    }


    @Override
    public View index() {
        if (getReturnIntegrationAdaptor() != null){
            return super.index();
        }else {
            if (TemplateEngine.getInstance(getTemplateDirectory()).exists("/html/index.html")){
                return html("index");
            }else {
                return super.index();
            }
        }

    }

    @Override
    public View show(long id) {
        if (getReturnIntegrationAdaptor() != null){
            return super.show(id);
        }else {
            if (TemplateEngine.getInstance(getTemplateDirectory()).exists("/html/show.html")){
                return redirectTo("html/show?id="+id);
            }else {
                return super.show(id);
            }
        }

    }

    @Override
    public View edit(long id) {
        if (TemplateEngine.getInstance(getTemplateDirectory()).exists("/html/edit.html")){
            return redirectTo("html/edit?id="+id);
        }else {
            return super.edit(id);
        }

    }

    @Override
    public View blank() {
        if (getReturnIntegrationAdaptor() != null){
            return super.blank();
        }else {
            if (TemplateEngine.getInstance(getTemplateDirectory()).exists("/html/blank.html")){
                return redirectTo("html/blank");
            }else {
                return super.blank();
            }
        }
    }
}
