package com.venky.swf.views;

import com.venky.swf.path._IPath;

import java.io.IOException;

public class NoContentView extends View{
    public NoContentView(_IPath path) {
        super(path);
    }

    @Override
    public void write(int httpStatusCode) throws IOException {

    }
}
