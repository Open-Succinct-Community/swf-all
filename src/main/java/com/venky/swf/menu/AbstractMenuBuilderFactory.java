/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.routing.Config;

/**
 *
 * @author venky
 */
public abstract class AbstractMenuBuilderFactory {
    
    private static MenuBuilder builder = null;
    
    public static MenuBuilder getMenuBuilder(){
        if (builder != null){
            return builder;
        }
        synchronized (AbstractMenuBuilderFactory.class){
            if (builder == null){
                String className = Config.instance().getProperty(Config.MENU_BUILDER_CLASS);
                try { 
                    builder = (MenuBuilder)Class.forName(className).newInstance();
                }catch (Exception ex){
                    throw new RuntimeException(ExceptionUtil.getRootCause(ex));
                }
            }
        }
        return builder;
    }
}
