package com.venky.swf.plugins.collab.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.KeyCase;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.View;

public class OpenSearchController<M extends Model> extends ModelController<M> {
    public OpenSearchController(Path path) {
        super(path);
    }

    @Override
    @RequireLogin(false)
    public View search() {
        return super.search();
    }

    @Override
    @RequireLogin(false)
    public View search(String strQuery) {
        return super.search(strQuery);
    }

    @Override
    @RequireLogin(false)
    public View index(){
        return super.index();
    }
}
