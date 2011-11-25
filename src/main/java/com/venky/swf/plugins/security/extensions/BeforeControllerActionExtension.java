package com.venky.swf.plugins.security.extensions;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Query;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.security.db.model.RolePermission;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.routing.Path;

public class BeforeControllerActionExtension implements Extension {
	static {
		Registry.instance().registerExtension(Path.ALLOW_CONTROLLER_ACTION, new BeforeControllerActionExtension());
	}
	public void invoke(Object... context) {
		
		String controllerPathElementName = (String)context[0];
		String actionPathElementName = (String)context[1];
		User user = (User)context[2];
		
		Query permissionQuery = new Query(RolePermission.class);
		
		permissionQuery.select().where(" role_id in (select role_id from "+ Database.getInstance().getTable(UserRole.class).getTableName() +" where user_id = ?) " ,new BindVariable(user.getId()));
		permissionQuery.and(" (controller_path_element_name is null " +
		 						" or (controller_path_element_name = ? and action_path_element_name is null) " +
		 						" or (controller_path_element_name = ? and action_path_element_name = ?) )", 
		 						new BindVariable(controllerPathElementName),
		 						new BindVariable(controllerPathElementName),new BindVariable(actionPathElementName));
		List<RolePermission> permissions = permissionQuery.execute();
		if (permissions.isEmpty()){
			return ;
		}
		Collections.sort(permissions, new Comparator<RolePermission>() {

			public int compare(RolePermission o1, RolePermission o2) {
				int ret =  StringUtil.valueOf(o2.getControllerPathElementName()).compareTo(o1.getControllerPathElementName());
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getActionPathElementName()).compareTo(o1.getActionPathElementName());	
				}
				return ret;
			}
			
		});
		
		RolePermission effective = permissions.get(0);
		if (effective.isAllowed()){
			return;
		}
		
		throw new AccessDeniedException();
	}
}
