/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.views.controls._IControl;

/**
 *
 * @author venky
 */
public class CheckBox extends Input{
    /**
	 * 
	 */
	private static final long serialVersionUID = -6483565209752648614L;

	public CheckBox(){
        super();
        setValue(true);
        removeClass("form-control");
    }

    @Override
    protected String getInputType() {
        return "checkbox";
    }
     
    public void setChecked(Object value){
    	@SuppressWarnings("unchecked")
		TypeConverter<Boolean> converter = (TypeConverter<Boolean>) Database.getJdbcTypeHelper("").getTypeRef(Boolean.class).getTypeConverter();
        if (converter.valueOf(String.valueOf(value))){
            super.setProperty("checked", value);
        }else {
            super.remove("checked");
        }
    }
    
    public boolean isChecked(){
    	return super.containsKey("checked");
    }

    @Override
    public void setParent(_IControl parent){
        super.setParent(parent);
        TextBox hiddenTextBox = new TextBox() ;
        hiddenTextBox.setVisible(false);
        hiddenTextBox.setName(getName());
        hiddenTextBox.setValue(false);
        parent.addControl(hiddenTextBox);
    }
}
