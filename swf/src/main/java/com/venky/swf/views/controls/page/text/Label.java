/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import com.venky.swf.views.controls.page.layout.Div;

/**
 *
 * @author venky
 */
public class Label extends Div {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1200402798314532864L;
	public Label(String label){
        this();
        this.setText(label);
    }
    public Label(){
        super();
        removeClass(getDefaultCssClass());
    }
    
}
