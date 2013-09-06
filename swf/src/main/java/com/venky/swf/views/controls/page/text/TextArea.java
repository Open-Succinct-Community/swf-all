package com.venky.swf.views.controls.page.text;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.util.WordWrapUtil;
import com.venky.swf.views.controls.Control;

public class TextArea extends Control implements _IAutoCompleteControl{

	public TextArea() {
		super("textarea", new String[]{"rows","1","cols",String.valueOf(ModelReflector.MAX_DATA_LENGTH_FOR_TEXT_BOX)});
	}
	
	@Override
	public void setText(String value) {
		super.setText(StringEscapeUtils.escapeHtml4(StringUtil.valueOf(value)));
		fixRows();
	}
	public void fixRows(){
		setRows(WordWrapUtil.getNumRowsRequired(getText(), getCols()));
	}
	public void setCols(int cols){
		setProperty("cols", cols);
	}
	public int getCols(){
		int cols = Integer.valueOf(getProperty("cols","80"));
		return (cols == 0 ? 80 : cols);
	}
	public void setRows(int rows){
		setProperty("rows", rows);
	}
	public int getRows(){
		int rows = (Integer.valueOf(getProperty("rows","1")));
		return (rows == 0 ? 1 : rows);
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
