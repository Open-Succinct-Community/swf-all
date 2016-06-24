package com.venky.swf.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.routing.Config;

public class ResetConfigHelper implements Extension{
	static {
		Registry.instance().registerExtension("com.venky.swf.routing.Router.shutdown", new ResetConfigHelper());
	}
	@Override
	public void invoke(Object... context) {
		Config.reset();
	}

}
