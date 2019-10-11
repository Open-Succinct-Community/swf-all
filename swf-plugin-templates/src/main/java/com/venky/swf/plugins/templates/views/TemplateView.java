package com.venky.swf.plugins.templates.views;

import com.venky.swf.path._IPath;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import org.json.simple.JSONObject;

public class TemplateView extends HtmlView {
    String templateName = null;
    JSONObject data = null;

    public TemplateView(_IPath path, String templateName){
        this(path,templateName,null);
    }
    public TemplateView(_IPath path, String templateName, JSONObject data) {
        super(path);
        this.templateName = templateName;
        this.data = data;
        if (this.data == null){
            this.data = new JSONObject();
        }
    }

    @Override
    protected void createBody(_IControl b) {
        b.addControl( new Dummy(TemplateEngine.getInstance().publish(templateName,data)));

    }

    public static class Dummy extends Control {

        String string = null;
        public Dummy(String string) {
            super("dummy");
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}