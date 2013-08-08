package com.venky.swf.views.controls.page.text;

import com.venky.cache.Cache;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.layout.DivOptionGroup;
import com.venky.swf.views.controls.page.text.RadioGroup.Radio;


public class RadioGroup extends DivOptionGroup<Radio>{

	private static final long serialVersionUID = -7730477350367195429L;

	Cache<String,Radio> valueRadioMap = new Cache<String, RadioGroup.Radio>() {

		private static final long serialVersionUID = 8020138556369377168L;

		@Override
		protected Radio getValue(String k) {
			return new Radio(k);
		}
	};
    public void setEnabled(final boolean enabled){
    	for (_IControl c : getContainedControls()){
    		if (c instanceof Radio){
    			Radio r = (Radio)c;
    			r.setEnabled(enabled);
    		}
    	}
    }
    public void setReadOnly(final boolean readonly){
    	for (_IControl c : getContainedControls()){
    		if (c instanceof Radio){
    			Radio r = (Radio)c;
    			r.setReadOnly(readonly);
    		}
    	}
    }
    public void setForm(String formId){
    	for (_IControl c : getContainedControls()){
    		if (c instanceof Radio){
    			Radio r = (Radio)c;
    			r.setForm(formId);
    		}
    	}
    }
    
	public Radio createOption(String text,String value){
		Radio radio = valueRadioMap.get(value);
		radio.setText(text);
		addControl(radio);
		if (name != null){
			radio.setName(name);
		}
		return radio;
	}
	
	private String name = null;
	@Override
	public void setName(String name){
		this.name = name;
		for (_IControl c :getContainedControls()){
			if (c instanceof Radio){
				Radio r = (Radio)c;
				r.setName(name);
			}
		}
	}
	@Override
	public void setValue(Object value){
		for (_IControl c :getContainedControls()){
			if (c instanceof Radio){
				Radio r = (Radio)c;
				r.setChecked(false);
			}
		}
		valueRadioMap.get(StringUtil.valueOf(value)).setChecked(true);
	}
	
	public static class Radio extends Input {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2609222149783319414L;

		public Radio(Object value) {
			super();
			setProperty("value", StringUtil.valueOf(value));
		}

		@Override
		protected String getInputType() {
			return "radio";
		}

		public void setChecked(Object value){
	    	@SuppressWarnings("unchecked")
			TypeConverter<Boolean> converter = (TypeConverter<Boolean>) Database.getJdbcTypeHelper().getTypeRef(Boolean.class).getTypeConverter();
	        if (converter.valueOf(String.valueOf(value))){
	            super.setProperty("checked", value);
	        }else {
	            super.remove("checked");
	        }
	    }

	}
}
