package com.venky.swf.plugins.wiki.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.views.controls.page.Css;
import com.venky.swf.views.controls.page.Head;
import com.venky.swf.views.controls.page.Script;

public class SyntaxHighlighterExtension implements Extension{
	
	static {
		SyntaxHighlighterExtension ext = new SyntaxHighlighterExtension();
		Registry.instance().registerExtension("after.create.head.pages/view", ext);
		Registry.instance().registerExtension("after.create.head.pages/show", ext);
	}

	@Override
	public void invoke(Object... context) {
		//Path path = (Path)context[0];
		Head head = (Head)context[1];
		head.addControl(new Script("/resources/scripts/syntaxhighlighter/js/shCore.js"));
		head.addControl(new Script("/resources/scripts/syntaxhighlighter/js/shBrushJava.js"));
		head.addControl(new Css("/resources/scripts/syntaxhighlighter/css/shCoreDefault.css"));
		head.addControl(new Script("/resources/scripts/swf/js/sh.js"));
		
	}

}
