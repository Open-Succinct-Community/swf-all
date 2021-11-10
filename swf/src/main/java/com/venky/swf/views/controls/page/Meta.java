package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

public class Meta extends Control {
    public Meta(String name,String content) {
        super("meta","property", name, "content", content);
    }
}
