/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import java.lang.annotation.Annotation;
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
        appmenu.createMenuItem(user.getName(), userMenu(user));
        addAppMenuItems(appmenu);
        return appmenu;
    }
    protected void addAppMenuItems(Menu appmenu){
        appmenu.createMenuItem("Manage", modelMenus(null));
    }
    
    protected Menu userMenu(User user){
        Menu logout = new Menu();
        logout.createMenuItem("Settings", "/user/edit/" +user.getId());
        logout.createMenuItem("Signout", "/logout");
        return logout;
    }
    
    protected Menu modelMenus(Class<? extends Annotation> annotationClass){
        Set<String> tableNames = Database.getInstance().getTableNames();
        Menu modelMenu = new Menu();
        for (String tableName : tableNames){
            Table<?> table = Database.getInstance().getTable(tableName);
            createMenuItem(modelMenu, table,annotationClass);
        }
        return modelMenu;
    }

    public void createMenuItem(Menu modelMenu,Table<?> table,Class<? extends Annotation> annotationClass){
    	Class<? extends Model> modelClass = table.getModelClass();
    	if (!canAddToMenu(modelClass)){
    		return;
    	}
    	if (annotationClass == null || ModelReflector.instance(modelClass).getAnnotation(annotationClass) != null){
            String modelName = modelClass.getSimpleName();
            modelMenu.createMenuItem(modelName, "/" + table.getTableName().toLowerCase());
    	}
    }
    
    protected boolean canAddToMenu(Class<? extends Model> modelClass){
    	return true;
    }
}
