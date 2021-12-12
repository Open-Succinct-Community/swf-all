package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

public class Meta extends Control {
    public Meta(String name,String content){
        this(name,content,false);
    }
    public Meta(String name,String content,boolean isProperty) {
        super("meta",isProperty ? "property" : "name", name, "content", content);
    }
}
