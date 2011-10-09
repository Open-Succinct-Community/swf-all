/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page;

import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class Image extends Control{
    public Image(String imageUrl){
        super("img");
        setProperty("src", imageUrl);
    }
    
}
