package com.venky.swf.plugins.wiki.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.views.controls.page.Css;
import com.venky.swf.views.controls.page.Head;
import com.venky.swf.views.controls.page.Script;

public class SyntaxHighlighterExtension implements Extension{
	static {
		Registry.instance().registerExtension("pages/view", new SyntaxHighlighterExtension());
	}

	@Override
	public void invoke(Object... context) {
		Head head = (Head)context[0];
		head.addControl(new Script("/resources/scripts/syntaxhighlighter/js/shCore.js"));
		head.addControl(new Script("/resources/scripts/syntaxhighlighter/js/shBrushJava.js"));
		head.addControl(new Css("/resources/scripts/syntaxhighlighter/css/shCoreDefault.css"));
		head.addControl(new Script("/resources/scripts/swf/js/sh.js"));
		
	}

}
