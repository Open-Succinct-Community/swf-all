package com.venky.swf.views.controls.page.text;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.string.StringUtil;
import com.venky.swf.views.controls.Control;

public class TextArea extends Control implements _IAutoCompleteControl{

	public TextArea() {
		super("textarea", new String[]{"rows","10","cols","80"});
	}
	
	@Override
	public void setText(String value) {
		super.setText(StringEscapeUtils.escapeHtml4(StringUtil.valueOf(value)));
	}

	public void setAutocompleteServiceURL(String autoCompleteServiceURL){
        setProperty("autoCompleteUrl", autoCompleteServiceURL);	
    }
	public void setOnAutoCompleteSelectProcessingUrl(String url){
		setProperty("onAutoCompleteSelectUrl",url);
	}
	@Override
    public void setValue(final Object value){
		setText(StringUtil.valueOf(value));
	}
	
	@Override
	public String getValue(){
		return getText();
	}

	private static final long serialVersionUID = 7419554617119038841L;
}
