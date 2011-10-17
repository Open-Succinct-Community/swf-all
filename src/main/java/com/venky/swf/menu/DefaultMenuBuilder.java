/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.menu;

import java.util.Set;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
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
        appmenu.createMenuItem("Manage", modelMenu());
        return appmenu;
    }
    
    private Menu userMenu(User user){
        Menu logout = new Menu();
        logout.createMenuItem("Settings", "/user/edit/" +user.getId());
        logout.createMenuItem("Signout", "/logout");
        return logout;
    }
    
    private Menu modelMenu(){
        Set<String> tableNames = Database.getInstance().getTableNames();
        Menu modelMenu = new Menu();
        for (String tableName : tableNames){
            Table<?> table = Database.getInstance().getTable(tableName);
            String modelName = table.getModelClass().getSimpleName();
            modelMenu.createMenuItem(modelName, "/" + tableName.toLowerCase());
        }
        return modelMenu;
    }
    
}
