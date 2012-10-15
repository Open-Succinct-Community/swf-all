package com.venky.swf.db.platform;

import java.util.Properties;
import java.util.logging.Logger;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;

public class CloudFoundryPlatform implements Extension{
	@Override
	public void invoke(Object... context) {
		Properties info = (Properties)context[0];
		String serviceName = System.getProperty("cf.db.service");
		
		if (ObjectUtil.isVoid(serviceName)){
			return ;
		}
		
		String db = System.getProperty("cloud.services."+serviceName + ".connection.name");
		String host = System.getProperty("cloud.services."+serviceName + ".connection.host");
		String port = System.getProperty("cloud.services."+serviceName + ".connection.port");
		String user = System.getProperty("cloud.services."+serviceName + ".connection.user");
		String password = System.getProperty("cloud.services."+serviceName + ".connection.password");
		String type = System.getProperty("cloud.services."+serviceName + ".type");
		
		Logger.getLogger(CloudFoundryPlatform.class.getName()).info("type:" + type);
		String jdbcurl = null; 
		if (type.contains("postgres")){
			jdbcurl = "jdbc:postgresql://";	
		}else if (type.contains("mysql")){
			jdbcurl = "jdbc:mysql://";
		}else if (type.contains("derby")){
			jdbcurl = "jdbc:derby://";
		}
		jdbcurl = jdbcurl + host + ":" + port + "/" + db;
		
		if (!info.containsKey("url")){
			info.setProperty("url", jdbcurl);
		}
		if (!info.containsKey("username")){
			info.setProperty("username", user);
		}
		if (!info.containsKey("password")){
			info.setProperty("password", password);
		}
	}
}
