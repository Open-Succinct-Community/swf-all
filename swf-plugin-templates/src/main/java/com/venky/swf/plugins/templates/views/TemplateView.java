package com.venky.swf.plugins.templates.views;

import com.venky.swf.path._IPath;
import com.venky.swf.plugins.templates.util.templates.TemplateEngine;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Head;

import java.util.HashMap;
import java.util.Map;

public class TemplateView extends HtmlView {
    String templateName = null;
    String templateDir = null;
    Map<String,Object> data = null;
    boolean fragment = false;

    public TemplateView(_IPath path, String templateName){
        this(path,null,templateName);
    }
    public TemplateView(_IPath path, String templateDir, String templateName){
        this (path,templateDir,templateName,null);
    }
    public TemplateView(_IPath path, String templateName, Map<String, Object> data) {
        this(path,null,templateName,data);
    }
    public TemplateView(_IPath path, String templateDir, String templateName, Map<String,Object> data){
        this(path,templateDir,templateName,data,false);
    }
    public TemplateView(_IPath path, String templateDir, String templateName, Map<String,Object> data, boolean fragment) {
        super(path);
        this.templateDir = templateDir;
        this.templateName = templateName;
        this.data = data;
        this.fragment = fragment;
        if (this.data == null){
            this.data = new HashMap<>();
        }
    }

    @Override
    protected void _createHead(Head head) {
        if (fragment) {
            return;
        }
        super._createHead(head);
    }

    @Override
    protected void createBody(_IControl b) {
        b.addControl( new Dummy(TemplateEngine.getInstance(templateDir).publish(templateName,data)));

    }

    @Override
    protected void createHead(Head head) {
        super.createHead(head);
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
