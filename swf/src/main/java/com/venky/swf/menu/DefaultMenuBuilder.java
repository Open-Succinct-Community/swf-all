/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import java.util.Set;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.page.Menu;

/**
 *
 * @author venky
 */
public class DefaultMenuBuilder implements _IMenuBuilder{
    
    public Menu createAppMenu(_IPath path) {
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
		Set<String> tableNames = Database.getTableNames();
        for (String tableName : tableNames){
			Table<?> table = Database.getTable(tableName);
			addMenuItem(user,appmenu, table);
        }
        return ;
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
    

    protected void addMenuItem(User user, Menu appMenu, Table<?> table){
    	Class<? extends Model> modelClass = table.getModelClass();
    	ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
        MENU menu = ref.getAnnotation(MENU.class);

        if (menu == null) {
        	return ;
        }
        
    	String menuName= menu.value();

    	String controllerPathName = table.getTableName().toLowerCase();
        String target = "/" + controllerPathName  ;
        if (Path.canAccessControllerAction(user,controllerPathName,"index",null) ){
        	Menu subMenu = appMenu.getSubmenu(menuName);
            String modelName = modelClass.getSimpleName();
        	subMenu.createMenuItem(modelName, target);
        }
    }
    
}
