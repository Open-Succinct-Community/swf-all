package com.venky.swf.plugins.templates.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
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
        if (getIntegrationAdaptor() != null){
            return super.index();
        }else {
            if (TemplateEngine.getInstance(getTemplateDirectory()).exists("/html/index.html")){
                return html("index");
            }else {
                return super.index();
            }
        }

    }

}
