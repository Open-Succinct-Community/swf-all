package com.venky.swf.plugins.oauth.extensions;

import com.venky.extension.Registry;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Html;
import com.venky.swf.views.controls.page.LinkedImage;
import com.venky.swf.views.extension.ViewFinalizerExtension;

public class ExtLoginView extends ViewFinalizerExtension{
	static {
		Registry.instance().registerExtension("finalize.view/login", new ExtLoginView());
	}

	@Override
	public void finalize(HtmlView view, Html html) {
		Body body = (Body)findFirst("body",html);
		if (body != null){
			body.addControl(0,new LinkedImage("/resources/images/oid.png","/oid/login"));
		}
	}
	
	
	
	private _IControl findFirst(String tag,_IControl root){
		if (root.getTag().equals(tag)) {
			return root;
		}
		for (_IControl c : root.getContainedControls()){
			_IControl first = findFirst(tag, c) ;
			if (first != null){
				return first;
			}
		}
		return null;
	}

	

}
