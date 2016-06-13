package com.venky.swf.db.platform;

import java.util.Properties;

import com.venky.extension.Extension;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.routing.Config;

public class SWFLocalPlatform implements Extension{

	public void invoke(Object... context) {
		Properties info = (Properties)context[0];
		if (!info.containsKey("pool")) info.setProperty("pool","");
		String pool = info.getProperty("pool");
		if (!info.containsKey("url")) info.setProperty("url", Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".url")));
		if (!info.containsKey("username")) info.setProperty("username", Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".userid")));
		if (!info.containsKey("password")) info.setProperty("password",Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".password")));
	}
}
