package com.venky.swf.views.controls.page.text;

import com.venky.swf.views.controls.Control;


public abstract class OptionCreator<T> extends Control {
	public OptionCreator(String tag, String... pairs) {
		super(tag, pairs);
	}

	private static final long serialVersionUID = 2323739582878342507L;

	public abstract T createOption(String text,String value);
}
