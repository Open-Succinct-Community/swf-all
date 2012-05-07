/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.path._IPath;

/**
 *
 * @author venky
 */
public abstract class View implements _IView{
    private _IPath path;
    public View(_IPath path){
        this.path = path;
    }

    public _IPath getPath() {
        return path;
    }
    public boolean isBeingRedirected(){
    	return false;
    }
    
}
