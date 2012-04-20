/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Menu;

/**
 *
 * @author venky
 */
public class DefaultMenuBuilder implements MenuBuilder{
    
    public Menu createAppMenu(Path path) {
        Menu appmenu  = new Menu();
        User user = (User)path.getSession().getAttribute("user");
        createUserMenu(appmenu,user);
        createApplicationMenu(appmenu,user);
        return appmenu;
    }
    protected void createUserMenu(Menu appmenu, User user){
    	Menu userMenu = userMenu(user);
        userMenu.createMenuItem("Signout", "/logout");
        appmenu.createMenuItem("Welcome " + user.getName(), userMenu);
    }

    protected void createApplicationMenu(Menu appmenu, User user){
    	Menu modelMenu = modelMenu(user,null);
    	if (!modelMenu.isEmpty()){
            appmenu.createMenuItem("Manage", modelMenu);
    	}
    }

    protected Menu userMenu(User user){
        Menu userMenu = new Menu();
        
        if (Path.canAccessControllerAction(user, "users", "edit", String.valueOf(user.getId()))){
            userMenu.createMenuItem("Settings", "/users/edit/" +user.getId());
        }else if (Path.canAccessControllerAction(user, "users", "show", String.valueOf(user.getId()))){
        	userMenu.createMenuItem("Settings", "/users/show/" +user.getId());
        }
        
        return userMenu;
    }
    
    protected Menu modelMenu(User user,Class<? extends Annotation> annotationFilter){
    	Set<String> tableNames = Database.getInstance().getTableNames();
        Menu modelMenu = new Menu();
        for (String tableName : tableNames){
            Table<?> table = Database.getInstance().getTable(tableName);
            addMenuItem(user,modelMenu, table, annotationFilter);
        }
        return modelMenu;
    }

    protected void addMenuItem(User user, Menu modelMenu, Table<?> table, Class<? extends Annotation> annotationFilter){
    	Class<? extends Model> modelClass = table.getModelClass();
    	if (!matches(modelClass, annotationFilter)){
    		return;
    	}
    	String controllerPathName = table.getTableName().toLowerCase();
        String target = "/" + controllerPathName  ;
        if (Path.canAccessControllerAction(user,controllerPathName,"index",null) ){
            String modelName = modelClass.getSimpleName();
        	modelMenu.createMenuItem(modelName, target);
        }
    }
    
    protected  boolean matches(Class<? extends Model> modelClass, Class<? extends Annotation> annotationFilter){
    	if (annotationFilter == null){
    		return true;
    	}
    	
    	ModelReflector ref = ModelReflector.instance(modelClass);
        if (!ref.isAnnotationPresent(modelClass,annotationFilter)) {
        	return false;
        }
        
        Annotation a = ref.getAnnotation(modelClass,annotationFilter);
        
        try {
			Method value = annotationFilter.getDeclaredMethod("value");
			if (Boolean.class.isAssignableFrom(value.getReturnType()) || boolean.class.isAssignableFrom(value.getReturnType())) {
				Boolean b = Boolean.valueOf(String.valueOf(value.invoke(a)));
				return b;
			}
		} catch (Exception e) {
			//
		}
        
        return true;
    }
    
    
}
