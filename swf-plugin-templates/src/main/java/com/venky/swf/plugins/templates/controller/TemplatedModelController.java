package com.venky.swf.plugins.templates.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;

public class TemplatedModelController<M extends Model> extends ModelController<M> implements TemplateLoader {
    public TemplatedModelController(Path path) {
        super(path);
    }

    @Override
    public String getTemplateDirectory() {
        return getReflector().getTableName().toLowerCase();
    }
}
