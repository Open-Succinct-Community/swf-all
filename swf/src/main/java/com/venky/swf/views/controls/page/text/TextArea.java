package com.venky.swf.views.controls.page.text;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.string.StringUtil;
import com.venky.swf.views.controls.Control;

public class TextArea extends Control{

	public TextArea() {
		super("textarea", new String[]{"rows","10","cols","80"});
	}
	
	@Override
	public void setText(String value) {
		super.setText(StringEscapeUtils.escapeHtml4(StringUtil.valueOf(value)));
	}

	private static final long serialVersionUID = 7419554617119038841L;
}
