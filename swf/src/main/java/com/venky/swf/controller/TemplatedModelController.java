package com.venky.swf.controller;

import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;

@Deprecated
public class TemplatedModelController<M extends Model> extends ModelController<M> implements TemplateLoader {
    public TemplatedModelController(Path path) {
        super(path);
    }


}
