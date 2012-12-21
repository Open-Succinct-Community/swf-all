package com.venky.swf.db.platform;

import java.util.Properties;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;

public class CloudFoundryPlatform implements Extension{
	
	public void invoke(Object... context) {
		Properties info = (Properties)context[0];
		String vcap_services = System.getenv("VCAP_SERVICES");
		String serviceName = System.getProperty("cf.db.service");

		Logger.getLogger(CloudFoundryPlatform.class.getName()).fine("vcap_services:" + vcap_services);
		Logger.getLogger(CloudFoundryPlatform.class.getName()).fine("cf.db.service:" +serviceName);
		if (ObjectUtil.isVoid(vcap_services)){
			return ;
		}

		if (ObjectUtil.isVoid(serviceName)){
			return ;
		}
		
		parse(serviceName,vcap_services,info);
	}
	
	public static void parse(String serviceName,String vcap_services,Properties info){
		JSONObject service = null; 
		
		JSONParser parser = new JSONParser();
		try {
			JSONObject jsvcapservices =  (JSONObject)parser.parse(vcap_services);
			if (jsvcapservices == null){
				return ;
			}
			JSONArray arr = (JSONArray)jsvcapservices.get(serviceName);
			if (arr == null || arr.isEmpty()){
				return;
			}
			service = (JSONObject) arr.get(0);
			if (service == null){
				return;
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		JSONObject credentials = ((JSONObject)service.get("credentials"));
		String db = (String)credentials.get("name");
		String host = (String)credentials.get("host");
		Number port = (Number)credentials.get("port");
		String user = (String)credentials.get("user");
		String password = (String)credentials.get("password");

		
		String jdbcurl = null ; 
		if (serviceName.startsWith("postgres")){
			jdbcurl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
		}else if (serviceName.startsWith("mysql")){
			jdbcurl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?zeroDateTimeBehavior=convertToNull&sessionVariables=storage_engine=INNODB" ;
		}
		Logger.getLogger(CloudFoundryPlatform.class.getName()).info("jdbcurl:" + jdbcurl);
		//Logger.getLogger(CloudFoundryPlatform.class.getName()).info("Systemproperties" + System.getProperties().toString());
		//Logger.getLogger(CloudFoundryPlatform.class.getName()).info("SystemEnv" + System.getenv().toString());
		
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
