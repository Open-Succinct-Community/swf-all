/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.PackageUtil;
import com.venky.swf.menu.MenuBuilder;

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
                _instance.loadExtensions();
            }
        }
        return _instance;
    }
    
    public String getProperty(String name){
        return properties.getProperty(name);
    }
    
    private static final String CONTROLLER_PACKAGE_ROOT = "swf.controller.package.root";
    private static final String MODEL_PACKAGE_ROOT = "swf.db.model.package.root";
    private static final String MENU_BUILDER_CLASS = "swf.menu.builder.class";
    private static final String EXTENSION_PACKAGE_ROOT = "swf.extn.package.root";
    
    private List<String> modelPackages = null;
    private List<String> controllerPackages = null;
    private List<URL> resourceBaseurls = null;
    public List<URL> getResouceBaseUrls(){
    	return resourceBaseurls;
    }
    
    public List<String> getModelPackageRoots(){
    	if (modelPackages == null){
    		modelPackages = getPropertyValueList(MODEL_PACKAGE_ROOT);
    	}
    	return modelPackages;
    }
    
    public List<String> getControllerPackageRoots(){
    	if (controllerPackages == null){
    		controllerPackages = getPropertyValueList(CONTROLLER_PACKAGE_ROOT);
    	}
    	return controllerPackages;
    }
    
    private List<String> getPropertyValueList(String name){
    	List<String> values = new ArrayList<String>();
    	StringTokenizer tok = new StringTokenizer(properties.getProperty(name,""),",");
    	while (tok.hasMoreTokens()) {
    		values.add(tok.nextToken());
    	}
    	return values;
	}
    
    MenuBuilder builder = null;
    public MenuBuilder getMenuBuilder(){
    	if (builder != null){
            return builder;
        }
        synchronized (this){
            if (builder == null){
                String className = properties.getProperty(MENU_BUILDER_CLASS);
                try { 
                    builder = (MenuBuilder)Class.forName(className).newInstance();
                }catch (Exception ex){
                    throw new RuntimeException(ExceptionUtil.getRootCause(ex));
                }
            }
        }
        return builder;
    }
    
    
    private void loadExtensions(){
		for (String root : Config.instance().getPropertyValueList(EXTENSION_PACKAGE_ROOT)){
			for (URL url:Config.instance().getResouceBaseUrls()){
				for (String extnClassName : PackageUtil.getClasses(url, root.replace('.', '/'))){
					try {
						Class.forName(extnClassName);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
        	}
        }
    }
	
}
