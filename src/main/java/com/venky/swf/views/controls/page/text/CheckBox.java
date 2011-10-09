/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.views.controls.Control;

/**
 *
 * @author venky
 */
public class CheckBox extends Input{
    public CheckBox(){
        super();
        setValue(true);
    }

    @Override
    protected String getInputType() {
        return "checkbox";
    }
     
    public void setChecked(Object value){
        if (Boolean.valueOf(String.valueOf(value))){
            super.setProperty("checked", value);
        }else {
            super.remove("checked");
        }
    }

    @Override
    protected void setParent(Control parent){
        super.setParent(parent);
        TextBox hiddenTextBox = new TextBox() ;
        hiddenTextBox.setVisible(false);
        hiddenTextBox.setName(getName());
        hiddenTextBox.setValue(false);
        parent.addControl(hiddenTextBox);
    }
}
