/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

/**
 *
 * @author venky
 */
public class Css extends Link{
    public Css(String path){
        super("link",path);
        setProperty("rel", "stylesheet");
    }
}
