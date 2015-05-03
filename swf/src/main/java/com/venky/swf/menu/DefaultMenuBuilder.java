/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import java.util.Set;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.model.ModelAwareness;
import com.venky.swf.views.controls.page.Menu;

/**
 *
 * @author venky
 */
public class DefaultMenuBuilder implements _IMenuBuilder{
    
    public Menu createAppMenu(_IPath path) {
        Menu appmenu  = new Menu();
        
        User user = (User)path.getSessionUser();
        if (user != null){
        	createUserMenu(path,appmenu,user);
        }
        createApplicationMenu(path,appmenu,user);
        return appmenu;
    }
    protected void createUserMenu(_IPath path, Menu appmenu, User user){
    	Menu userMenu = userMenu(path,appmenu,user);
        userMenu.createMenuItem("Signout", "/logout");
    }

    protected void createApplicationMenu(_IPath path,Menu appmenu, User user){
		Set<String> tableNames = Database.getTableNames();
        for (String tableName : tableNames){
			Table<?> table = Database.getTable(tableName);
			addMenuItem(path,user,appmenu, table);
        }
        return ;
    }

    protected Menu userMenu(_IPath path, Menu appmenu, User user){
        Menu userMenu = appmenu.getSubmenu(user.getName());
        _IPath userPath = path.getModelAccessPath(User.class);
        
        if (userPath.canAccessControllerAction("edit", String.valueOf(user.getId()))){
            userMenu.createMenuItem("Settings", "/users/edit/" +user.getId());
        }else if (userPath.canAccessControllerAction("show", String.valueOf(user.getId()))){
        	userMenu.createMenuItem("Settings", "/users/show/" +user.getId());
        }
        
        return userMenu;
    }
    

    protected void addMenuItem(_IPath path,User user, Menu appMenu, Table<?> table){
    	Class<? extends Model> modelClass = table.getModelClass();
    	if (modelClass == null){
    		return; //Seems like some internal table of the db.
    	}
    	ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
        MENU menu = ref.getAnnotation(MENU.class);

        if (menu == null) {
        	return ;
        }
        
    	String menuName= menu.value();
    	if (ObjectUtil.isVoid(menuName)){
    		return;
    	}
    	_IPath modelAccessPath = path.getModelAccessPath(modelClass);
        
        if (modelAccessPath.canAccessControllerAction("index")){
        	Menu subMenu = appMenu.getSubmenu(menuName);
            String modelName = new ModelAwareness((Path) modelAccessPath, null).getLiteral(modelClass.getSimpleName());
        	subMenu.createMenuItem(modelName, modelAccessPath.controllerPath() + "/index");
        }
    }
    
}
