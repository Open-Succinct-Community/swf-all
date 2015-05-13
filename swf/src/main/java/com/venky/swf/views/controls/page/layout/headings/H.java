package com.venky.swf.views.controls.page.layout.headings;

import com.venky.swf.views.controls.Control;

public class H extends Control{

	public H(int number) {
		super("h" + number );
		addClass("h"+ number);
	}

	private static final long serialVersionUID = 6569684905097950191L;
}
