package com.venky.swf.views;

import com.venky.swf.path._IPath;
import com.venky.swf.util.PegDownProcessor;
import com.venky.swf.util.TemplateProcessor;
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

    public String getTemplateName() {
        return templateName;
    }

    public boolean isFragment() {
        return fragment;
    }

    public TemplateView(_IPath path, String templateName){
        this(path,null,templateName,null,false);
    }
    public TemplateView(_IPath path, String templateDir, String templateName){
        this (path,templateDir,templateName,null,false);
    }
    public TemplateView(_IPath path, String templateDir, String templateName, boolean fragment){
        this(path,templateDir,templateName,null,fragment);
    }

    public TemplateView(_IPath path, String templateName, Map<String, Object> data) {
        this(path,null,templateName,data, false);
    }
    public TemplateView(_IPath path, String templateDir, String templateName, Map<String,Object> data){
        this(path,templateDir,templateName,data,false);
    }
    private TemplateView(_IPath path, String templateDir, String templateName, Map<String,Object> data, boolean fragment) {
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
    public String toString() {
        if (!fragment) {
            return super.toString();
        }
        return publish();
    }

    protected String publish(){
        return TemplateProcessor.getInstance(templateDir).publish(templateName,data);
    }

    @Override
    protected void _createBody(_IControl body, boolean includeStatusMessage) {
        super._createBody(body, false);
    }

    @Override
    protected void createBody(_IControl b) {
        b.addControl( new Dummy(publish()));
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