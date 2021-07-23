package com.venky.swf.plugins.templates.controller;

import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.plugins.templates.views.TemplateView;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

import java.util.HashMap;

public class TemplatedController extends Controller implements TemplateLoader {
    public TemplatedController(Path path) {
        super(path);
    }


}
