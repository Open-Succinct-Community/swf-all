package com.venky.swf.db.platform;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.swf.routing.Config;

public class HerokuPlatform implements Extension{
	
	public void invoke(Object... context) {
		Properties info = (Properties)context[0];
		String dbURL = System.getenv("DATABASE_URL");
		if (!ObjectUtil.isVoid(dbURL)){
    		Config.instance().getLogger(HerokuPlatform.class.getName()).fine("DATABASE_URL:" + dbURL );
    		URI uri;
			try {
				uri = new URI(dbURL);
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			String jdbcurl = null; 
    		if (uri.getScheme().equals("postgres")){
    			jdbcurl = "jdbc:postgresql://";	
    		}else if (uri.getScheme().equals("mysql")){
    			jdbcurl = "jdbc:mysql://";
    		}else if (uri.getScheme().equals("derby")){
    			jdbcurl = "jdbc:derby://";
    		}
    		jdbcurl = jdbcurl + uri.getHost() + uri.getPath() ;
    		
    		if (!info.containsKey("url")){
    			info.setProperty("url", jdbcurl);
    		}
    		if (!info.containsKey("username")){
    			info.setProperty("username", uri.getUserInfo().split(":")[0]);
    		}
    		if (!info.containsKey("password")){
    			info.setProperty("password", uri.getUserInfo().split(":")[1]);
    		}
    	}
	}
}
