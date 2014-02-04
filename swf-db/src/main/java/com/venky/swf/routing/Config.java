/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.venky.cache.Cache;
import com.venky.core.log.TimerStatistics;
import com.venky.core.util.ObjectUtil;
import com.venky.core.util.PackageUtil;

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
    private List<URL> resourceBaseurls = null;
    public List<URL> getResourceBaseUrls(){
    	return resourceBaseurls;
    }
    
    public String getProperty(String name){
    	return System.getProperty(name,properties.getProperty(name));
    }
    public String getProperty(String name,String defaultValue){
    	return System.getProperty(name,properties.getProperty(name, defaultValue));
    }
    public int getIntProperty(String name){
    	String sValue = getProperty(name);
		return Integer.parseInt(sValue);
    }
    public int getIntProperty(String name,int defaultValue){
    	String sValue = getProperty(name, String.valueOf(defaultValue));
		return Integer.parseInt(sValue);
    }
    
    public List<String> getPackageRoots(String rootPackage){
    	return propertyValueList.get(rootPackage);
    }
    
    private static final String MODEL_PACKAGE_ROOT = "swf.db.model.package.root";
    public List<String> getModelPackageRoots(){
    	return getPackageRoots(MODEL_PACKAGE_ROOT);
    }
    private static final String EXTENSION_PACKAGE_ROOT = "swf.extn.package.root";
    public List<String> getExtensionPackageRoots(){
		return getPackageRoots(EXTENSION_PACKAGE_ROOT);
    }
    
    
    private static final String MENU_BUILDER_CLASS = "swf.menu.builder.class";
    String getMenuBuilderClassName(){
        return properties.getProperty(MENU_BUILDER_CLASS);
    }
    
    public List<String> getModelClasses(){ 
    	List<String> modelClasses = new ArrayList<String>();
		for (String root : getModelPackageRoots()) {
			for (URL url : getResourceBaseUrls()) {
        		modelClasses.addAll(PackageUtil.getClasses(url, root.replace('.', '/')));
			}
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
    
    private Cache<String,List<String>> propertyValueList = new Cache<String, List<String>>() {
		private static final long serialVersionUID = 4415548468945425620L;

		@Override
		protected List<String> getValue(String name) {
	    	List<String> values = new ArrayList<String>();
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
    
    public boolean isTimerEnabled(){
    	return getLogger(TimerStatistics.class.getName()).isLoggable(Level.FINE);
    }
    
    private Cache<String,Logger> loggers = new Cache<String, Logger>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7431631281937883673L;

		@Override
		protected Logger getValue(String k) {
			return Logger.getLogger(k);
		}
    	
    };
    public Logger getLogger(String name){
    	return loggers.get(name);
    }
    
	public boolean isDevelopmentEnvironment(){
		String environment = getProperty("swf.env","development");
		if ("development".equalsIgnoreCase(environment)){
			return true;
		}
		return false;
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
}
