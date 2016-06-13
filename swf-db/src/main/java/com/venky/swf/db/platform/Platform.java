package com.venky.swf.db.platform;

import java.util.Properties;

import com.venky.extension.Registry;

public class Platform {
	public static final String DETECT_PLATFORM_DB = "detect.platform.db";
	static {
		Registry.instance().registerExtension(DETECT_PLATFORM_DB, new HerokuPlatform());
		Registry.instance().registerExtension(DETECT_PLATFORM_DB, new CloudFoundryPlatform());
		Registry.instance().registerExtension(DETECT_PLATFORM_DB, new SWFLocalPlatform());
	}
	public static Properties getConnectionProperties(String pool) {
		Properties props = new Properties();
		props.put("pool",pool);
		Registry.instance().callExtensions(DETECT_PLATFORM_DB, props);
		return props;
	}
}
