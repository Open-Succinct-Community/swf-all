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

/**
 *
 * @author venky
 */
public class Config {
    private Config(){
        properties = new Properties();
        try {
            properties.load(getClass().getResourceAsStream("/config/swf.properties"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
    
    public String getProperty(String name){
        return properties.getProperty(name);
    }
    
    public static final String CONTROLLER_PACKAGE_ROOT = "swf.controller.package.root";
    public static final String MODEL_PACKAGE_ROOTS = "swf.db.model.package.roots";
    public static final String MENU_BUILDER_CLASS = "swf.menu.builder.class";
    
    private List<String> modelPackages = null;
    private List<URL> resourceBaseurls = null;
    public List<URL> getResouceBaseUrls(){
    	loadModelPackageRoots();
    	return resourceBaseurls;
    }
    
    public List<String> getModelPackageRoots(){
    	loadModelPackageRoots();
    	return modelPackages;
    }
    
    private void loadModelPackageRoots(){
    	if (modelPackages != null){
    		return;
    	}
	    Enumeration<URL> propertyFileUrls;
		try {
			propertyFileUrls = getClass().getClassLoader().getResources("config/swf.properties");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	    modelPackages = new ArrayList<String>();
	    resourceBaseurls = new ArrayList<URL>();
	    while(propertyFileUrls.hasMoreElements()){
	    	URL url = propertyFileUrls.nextElement();
			try {
				Properties one = new Properties();
				one.load((InputStream)url.getContent());
				for (String packageName : Config.instance().getModelPackageRoots(one)){
					if (!modelPackages.contains(packageName)){
						modelPackages.add(packageName);
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
    
    private List<String> getModelPackageRoots(Properties properties){
        List<String> modelPackages = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(properties.getProperty(MODEL_PACKAGE_ROOTS,""),",");
        while (tok.hasMoreTokens()){
            modelPackages.add(tok.nextToken());
        }
        return modelPackages;
    }
}
