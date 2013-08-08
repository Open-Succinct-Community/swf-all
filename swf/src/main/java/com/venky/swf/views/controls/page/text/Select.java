package com.venky.swf.views.controls.page.text;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.text.Select.Option;

public class Select extends OptionCreator<Option>{


	/**
	 * 
	 */
	private static final long serialVersionUID = 2372577592146683305L;

	public Select() {
		super("select");
	}
	
	public Option createOption(String text, String value){
		Option option = new Option(text, value);
		addControl(option);
		return option;
	}
	
	public static class Option extends Control {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2609222149783319414L;

		public Option(String text,Object value) {
			super("option");
			setText(text);
			setProperty("value", StringUtil.valueOf(value));
		}
		
	}
    public void setValue(final Object value){
    	for (_IControl control:getContainedControls()){
    		if (control instanceof Option){
    			Option o = (Option)control;
    			if (ObjectUtil.equals(o.getValue(),value)){
        			o.setProperty("selected", "selected");
    			}else {
    				o.remove("selected");
    			}
    		}
    	}
    }
    
    public String getValue(){
    	String value = null;
    	for (_IControl control:getContainedControls()){
    		if (control instanceof Option){
    			Option o = (Option)control;
    			if (value == null){
    				value = o.getValue();
    			}
    			if (ObjectUtil.equals(o.getProperty("selected"), "selected")){
    				return o.getValue();
    			}
    		}
    	}
    	return value;
    }

	
}
