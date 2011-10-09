/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

/**
 *
 * @author venky
 */
public class TextBox extends Input{
    
    public void setAutocompleteServiceURL(String autoCompleteServiceURL){
        setProperty("autoCompleteUrl", autoCompleteServiceURL);
    }
    
    public TextBox(){
        super();
    }

    @Override
    protected String getInputType() {
        return "text";
    }
    
}