package com.venky.swf.views.extension;

import com.venky.extension.Extension;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.page.Html;

public abstract class ViewFinalizerExtension implements Extension{

	public void invoke(Object... context) {
		HtmlView v = (HtmlView)context[0];
		Html html = (Html)context[1];
		finalize(v,html);
	}
	
	public abstract void finalize(HtmlView view,Html html);
}
