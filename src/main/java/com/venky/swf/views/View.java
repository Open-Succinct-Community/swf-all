/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.routing.Path;
import java.io.IOException;

/**
 *
 * @author venky
 */
public abstract class View {
    Path path;
    public View(Path path){
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public abstract void write() throws IOException;
}
