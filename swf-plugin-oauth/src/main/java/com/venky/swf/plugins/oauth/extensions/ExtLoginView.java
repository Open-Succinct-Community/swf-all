package com.venky.swf.plugins.oauth.extensions;

import com.venky.extension.Registry;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.extension.ViewFinalizerExtension;

public class ExtLoginView extends ViewFinalizerExtension{
	static {
		Registry.instance().registerExtension("finalize.view/login", new ExtLoginView());
	}

	@Override
	public void finalize(HtmlView view, Html html) {
		Body body = (Body)findFirst("body",html);
		if (body != null){
			Link openId = new Link();
			openId.setUrl("/oid/login");
			openId.addControl(new Image("/resources/images/oid.svg"));
			body.addControl(0,openId);
		}
	}
	
	
	
	private Control findFirst(String tag,Control root){
		if (root.getTag().equals(tag)) {
			return root;
		}
		for (Control c : root.getContainedControls()){
			Control first = findFirst(tag, c) ;
			if (first != null){
				return first;
			}
		}
		return null;
	}

	

}
