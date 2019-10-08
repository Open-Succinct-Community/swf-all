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

    public TemplateView(_IPath path, String templateName, JSONObject data) {
        super(path);
        this.templateName = templateName;
        this.data = data;
    }

    @Override
    protected void createBody(_IControl b) {
        b.addControl( new StringControl(TemplateEngine.getInstance().publish(templateName,data)));

    }

    public static class StringControl extends Control {

        String string = null;
        public StringControl(String string) {
            super("string");
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
