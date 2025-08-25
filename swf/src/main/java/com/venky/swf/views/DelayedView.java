package com.venky.swf.views;

import com.venky.swf.path._IPath;

import java.io.IOException;

public class DelayedView extends NoContentView{
    public DelayedView(_IPath path) {
        super(path);
    }
    
    @Override
    public boolean isBeingForwarded() {
        return true;
    }
}
