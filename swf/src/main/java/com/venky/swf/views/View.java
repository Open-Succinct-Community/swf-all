/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.path.Path;

/**
 *
 * @author venky
 */
public abstract class View implements _IView{
    private Path path;
    public View(Path path){
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
    public boolean isBeingRedirected(){
    	return false;
    }
    
}
