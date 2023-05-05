/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views;

import com.venky.swf.db.Database;
import com.venky.swf.path._IPath;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
        if (path == null){
            return Database.getInstance().getContext(_IPath.class.getName());
        }
        return path;
    }
    public boolean isBeingRedirected(){
    	return false;
    }

    public void setPath(_IPath path){
        synchronized (this) {
            this.path = path;
        }
    }

    public void write() throws IOException {
        write(HttpServletResponse.SC_OK);
    }
    
}
