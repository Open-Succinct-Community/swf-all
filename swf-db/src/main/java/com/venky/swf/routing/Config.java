/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.log.SWFLogger;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.core.util.PackageUtil;
import com.venky.swf.integration.api.Call;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author venky
 */
public class Config {
	private Config(){
        properties = new Properties();

        Enumeration<URL> propertyFileUrls;
		try {
			propertyFileUrls = getClass().getClassLoader().getResources("config/swf.properties");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
	    resourceBaseurls = new ArrayList<URL>();
	    while(propertyFileUrls.hasMoreElements()){
	    	URL url = propertyFileUrls.nextElement();
			try {
				Properties one = new Properties();
				one.load((InputStream)url.getContent());
				for (Object key : one.keySet()){
					String nValue = one.getProperty((String)key);
					String oValue = properties.getProperty((String)key);
					if (oValue == null){
						properties.put(key, nValue);
					}else {
						properties.put(key, oValue+","+nValue);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
            try {
                url = new URL(url.toString().substring(0,url.toString().length()-"config/swf.properties".length()));
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
            resourceBaseurls.add(url);
	    }
	    properties.putAll(System.getProperties());
	    properties.putAll(System.getenv());
    }
    private Properties properties;
    private static Config _instance ;
    
    public static Config instance(){
        if (_instance != null){
            return _instance;
        }
        synchronized (Config.class){
            if (_instance == null) {
                _instance = new Config();
            }
        }
        return _instance;
    }
	public static void reset(){
		synchronized (Config.class) {
			_instance =null;
		}
	}	

	ThreadLocal<String> host= new ThreadLocal<>();
    public void setHostName(String hostName){
    	if (hostName == null){
    		host.remove();
		}else {
			host.set(hostName);
		}
	}

	public String getHostName(){
    	loadExternalIp();
    	if (host.get() != null){
    		return host.get();
		}else {
			return getProperty("swf.host","localhost");
		}
	}

	public void loadExternalIp(){
		if (properties.getProperty("swf.host") == null){
			String externalIp = StringUtil.read(new Call<String>().url("https://api.ipify.org/").getResponseStream());
			properties.put("swf.host", externalIp);
		}
	}
	public int getPortNumber(){
    	return Integer.parseInt(getPort());
	}

	private String getPort(){
		return getProperty("swf.port",getProperty("PORT","8080"));
	}

	ThreadLocal<String> externalPort = new ThreadLocal<>();
	public void setExternalPort(String portNumber){
		if (ObjectUtil.isVoid(portNumber)){
			externalPort.remove();
		}else {
			externalPort.set(portNumber);
		}
	}

	private String getExternalPort(){
		if (externalPort.get() != null){
			return externalPort.get();
		}
    	return getProperty("swf.external.port",getPort());
	}

	ThreadLocal<String> uriScheme = new ThreadLocal<>();
	public void setExternalURIScheme(String uriScheme){
		if (uriScheme == null){
			this.uriScheme.remove();
		}else {
			this.uriScheme.set(uriScheme);
		}
	}
	public String getExternalURIScheme(){
		if (uriScheme.get() != null){
			return uriScheme.get();
		}
		return getProperty("swf.external.scheme", getExternalPortNumber() == 443 ? "https" : "http" );
	}

	private int getExternalPortNumber(){
    	if (ObjectUtil.isVoid(getExternalPort())){
			String bareScheme = getProperty("swf.external.scheme");
    		if (ObjectUtil.equals("http",bareScheme)){
    			return 80;
			}else if (ObjectUtil.equals("https",bareScheme)){
				return 443;
			}else {
				return 80;
			}
		}else {
			return Integer.parseInt(getExternalPort());
		}
	}

	public String getServerBaseUrl(){
    	String protocol = getExternalURIScheme();
		StringBuilder url = new StringBuilder().append(protocol).append("://").append(getHostName());

		int externalPortNumber = getExternalPortNumber();
		if (externalPortNumber != 80 && externalPortNumber != 443){
			url.append(":").append(getExternalPortNumber());
		}

		return url.toString();
	}
	public List<String> getOpenIdProviders(){
		List<String> openIdProviders = new SequenceSet<>();
		for (String k : getPropertyKeys("swf\\.[A-Z]*\\.client\\.id")){
			if (getProperty(k) != null) {
				// Youi may need property to say it is a openid client id ..!
				//Can add a property swf.provider.client.openid=true
				openIdProviders.add(k.split("\\.")[1]);
			}
		}
		return openIdProviders;
	}
	public String getClientId(String opendIdProvider){
		return getProperty("swf."+opendIdProvider +".client.id");
	}
	public String getClientSecret(String opendIdProvider){
		return getProperty("swf."+opendIdProvider +".client.secret");
	}
	
    private List<URL> resourceBaseurls = null;
    public List<URL> getResourceBaseUrls(){
    	return resourceBaseurls;
    }
    
    public String getProperty(String name){
    	return getProperty(name,null); 
    }
    public String getProperty(String name,String defaultValue){
    	return properties.getProperty(name, defaultValue);
    }

    public void setProperty(String name, String value){
    	properties.setProperty(name,value);
    	propertyValueList.remove(name); //Let cache work it out.
	}
	public void removeProperty(String name){
		properties.remove(name);
		propertyValueList.remove(name); //Let cache work it out.
	}

    public int getIntProperty(String name){
    	String sValue = getProperty(name);
		return Integer.parseInt(sValue);
    }
    public int getIntProperty(String name,int defaultValue){
    	String sValue = getProperty(name, String.valueOf(defaultValue));
		return Integer.parseInt(sValue);
    }
    public long getLongProperty(String name, long defaultValue){
		String sValue = getProperty(name, String.valueOf(defaultValue));
		return Long.parseLong(sValue);
	}
    public boolean getBooleanProperty(String name){
    	String sValue = getProperty(name);
    	return Boolean.parseBoolean(sValue);
    }
    public boolean getBooleanProperty(String name, boolean defaultValue){
    	String sValue = getProperty(name,String.valueOf(defaultValue));
    	return Boolean.parseBoolean(sValue);
    }
    public List<String> getPackageRoots(String rootPackage){
    	return getPropertyValueList(rootPackage);
    }
    
    private static final String MODEL_PACKAGE_ROOT = "swf.db.model.package.root";
    public List<String> getModelPackageRoots(){
    	return getPackageRoots(MODEL_PACKAGE_ROOT);
    }
    private static final String EXTENSION_PACKAGE_ROOT = "swf.extn.package.root";
    public List<String> getExtensionPackageRoots(){
		return getPackageRoots(EXTENSION_PACKAGE_ROOT);
    }

	public List<String> getPropertyKeys(String regEx){
		List<String> keys = new ArrayList<String>();
		Pattern pattern = Pattern.compile(regEx);

		for (Object key: properties.keySet()){
			String sKey = StringUtil.valueOf(key);
			if (pattern.matcher(sKey).matches()) {
				keys.add(sKey);
			}
		}
		return keys;
	}
    
    private static final String MENU_BUILDER_CLASS = "swf.menu.builder.class";
    String getMenuBuilderClassName(){
        return properties.getProperty(MENU_BUILDER_CLASS);
    }
    
    private Cache<String,List<String>> sNToFQNs = new Cache<String, List<String>>(Cache.MAX_ENTRIES_UNLIMITED,Cache.PRUNE_FACTOR_DEFAULT) {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8286215452116271529L;

		@Override
		protected List<String> getValue(String k) {
			return new ArrayList<String>();
		}
	};
	
	private void loadModelClasses(){
		if (sNToFQNs.size() > 0){
			return ;
		}
		for (String root : getModelPackageRoots()) {
			for (URL url : getResourceBaseUrls()) {
        		for (String cn: PackageUtil.getClasses(url, root.replace('.', '/'))){
        			sNToFQNs.get(cn.substring(cn.lastIndexOf('.')+1)).add(cn);
        		}
			}
		}
	}
	public List<String> getModelClasses(String simpleModelName){
		loadModelClasses();
		return sNToFQNs.get(simpleModelName);
	}
    public List<String> getModelClasses(){
    	loadModelClasses();
    	List<String> modelClasses = new ArrayList<String>();
		for (List<String> fQNs : sNToFQNs.values()) {
    		modelClasses.addAll(fQNs);
		}
		return modelClasses;
    }
    
    private static final String CONFIGURATION_INSTALLERS = "swf.default.configuration.installer";
    
    private List<String> installers = null;
    public List<String> getInstallers(){
    	if (installers == null){
    		installers = getPropertyValueList(CONFIGURATION_INSTALLERS);
    		Collections.reverse(installers);// To make sure framework installers are installed first.
    	}
    	return installers;
    }
    
    private Cache<String,List<String>> propertyValueList = new Cache<String, List<String>>(0,0) {
		private static final long serialVersionUID = 4415548468945425620L;

		@Override
		protected List<String> getValue(String name) {
	    	List<String> values = new SequenceSet<>();
	    	StringTokenizer tok = new StringTokenizer(properties.getProperty(name,""),",");
	    	while (tok.hasMoreTokens()) {
	    		values.add(tok.nextToken());
	    	}
	    	return values;
		}
	};
    
	private List<String> getPropertyValueList(String name){
    	return propertyValueList.get(name);
	}
    
    
    
    private Cache<String,SWFLogger> loggers = new Cache<String, SWFLogger>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7431631281937883673L;

		@Override
		protected SWFLogger getValue(String k) {
			return new SWFLogger(Logger.getLogger(k));
		}
    	
    };
    public SWFLogger getLogger(String name){
    	return loggers.get(name);
    }
    
	public boolean isDevelopmentEnvironment(){
		String environment = getProperty("swf.env","development");
        return "development".equalsIgnoreCase(environment);
    }

	private Boolean timerAdditive = null;
	public boolean isTimerAdditive(){
		if (timerAdditive == null){
			timerAdditive = Boolean.valueOf(getProperty("swf.timer.additive", "true")); 
		}
		return timerAdditive; 
	}
	
	public void printStackTrace(Class<?> fromClazz, Throwable th){
        StringWriter sw = new StringWriter();
        PrintWriter w = new PrintWriter(sw);
        if (isDevelopmentEnvironment() || ObjectUtil.isVoid(th.getMessage())){
            th.printStackTrace(w);
        }else {
        	w.write(th.getMessage());
        }
		getLogger(fromClazz.getName()).fine(sw.toString());
	}

	public Map<String,String> getGeoProviderParams(){
		Map<String,String> params  =new HashMap<>();
		params.put("here.app_id",Config.instance().getProperty("geocoder.here.app_id"));
		params.put("here.app_code",Config.instance().getProperty("geocoder.here.app_code"));
		params.put("here.app_key",Config.instance().getProperty("geocoder.here.app_key"));

		params.put("google.api_key",Config.instance().getProperty("geocoder.google.api_key"));
		return params;
	}

	public boolean shouldPasswordsBeEncrypted(){
		return getBooleanProperty("swf.user.password.encrypted",false);
	}

	ThreadLocal<Boolean> rootElementNameRequiredForApis = new ThreadLocal<>();
	public void setRootElementNameRequiredForApis(Boolean required){
		if (required == null){
			rootElementNameRequiredForApis.remove();
		}else {
			rootElementNameRequiredForApis.set(required);
		}
	}

	public boolean isRootElementNameRequiredForApis(){
		Boolean required = rootElementNameRequiredForApis.get();
		if (required == null){
			required = getBooleanProperty("swf.api.root.required",true);
		}
		return required;
	}
	ThreadLocal<KeyCase> apiKeyCase = new ThreadLocal<>();

	public void setApiKeyCase(KeyCase keyCase) {
		if (keyCase == null){
			apiKeyCase.remove();
		}else {
			apiKeyCase.set(keyCase);
		}
	}

	public KeyCase getApiKeyCase(){
		KeyCase keyCase = apiKeyCase.get();
		if (keyCase == null){
			keyCase = KeyCase.valueOf(Config.instance().getProperty("swf.api.keys.case",KeyCase.CAMEL.toString()));
		}
		return keyCase;
	}


	public List<String> getMonitorClassesLoaded(){
		return Config.instance().getPropertyValueList("swf.classloader.debug.classes");
	}

}
