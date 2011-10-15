/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.routing;

import java.io.IOException;
import java.util.ArrayList;
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
    public List<String> getModelPackageRoots(){
        List<String> modelPackages = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(Config.instance().getProperty(MODEL_PACKAGE_ROOTS),",");
        while (tok.hasMoreTokens()){
            modelPackages.add(tok.nextToken());
        }
        modelPackages.add("com.venky.swf.db.model");
        return modelPackages;
    }
}
