package com.venky.swf.plugins.security.extensions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Query;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.security.db.model.RolePermission;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.routing.Path;

public class ParticipantControllerAccessExtension implements Extension{
	static {
		Registry.instance().registerExtension(Path.ALLOW_CONTROLLER_ACTION, new ParticipantControllerAccessExtension());
	}

	public void invoke(Object... context) {
		User user = (User)context[0];
		String controllerPathElementName = (String)context[1];
		String actionPathElementName = (String)context[2];
		String parameterValue = (String)context[3];
		String possibleTableName = StringUtil.camelize(controllerPathElementName);
		Class<? extends Model> modelClass  = null;
		List<String> participantingRoles = new ArrayList<String>();

		Table possibleTable = Database.getInstance().getTable(possibleTableName);
		if ( possibleTable != null ){
			modelClass = possibleTable.getModelClass();
		}
		if (modelClass != null && parameterValue != null){
			try {
				int id = Integer.valueOf(parameterValue);
				Model model = possibleTable.get(id);
				Map<String,List<Integer>> pOptions = user.getParticipationOptions(modelClass);
				ModelReflector<? extends Model> reflector = ModelReflector.instance(modelClass);
				for (String referencedModelIdFieldName :pOptions.keySet()){
					Integer referenceValue = (Integer)reflector.getFieldGetter(referencedModelIdFieldName).invoke(model);
					if (pOptions.get(referencedModelIdFieldName).contains(referenceValue)){
						participantingRoles.add(referencedModelIdFieldName.substring(0, referencedModelIdFieldName.length()-2));
					}
				}
				if (!pOptions.isEmpty() && participantingRoles.isEmpty()){
					throw new AccessDeniedException(); // User is not a participant on the model.
				}
			}catch (NumberFormatException ex){
				//
			}catch (InvocationTargetException ex) {
				throw new RuntimeException(ex.getCause());
			}catch (Exception ex){
				throw new RuntimeException(ex);
			}
		}

		
		Query permissionQuery = new Query(RolePermission.class);
		String userRole = Database.getInstance().getTable(UserRole.class).getTableName() ; 
		
		permissionQuery.select().where(" role_id in (select role_id from "+ userRole +
														" where user_id = ? ) " ,new BindVariable(user.getId()));
		permissionQuery.and(" ( participation is null" );
		for (String participatingRole:participantingRoles){
			 permissionQuery.add(" or participation = ? ",new BindVariable(participatingRole));
		}
		permissionQuery.add(" )");
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
				int ret =  0; 
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getControllerPathElementName()).compareTo(o1.getControllerPathElementName());
				}
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getActionPathElementName()).compareTo(o1.getActionPathElementName());	
				}
				return ret;
			}
			
		});
		
		RolePermission firstPermission = permissions.get(0);
		
		Iterator<RolePermission> permissionIterator = permissions.iterator();
		while (permissionIterator.hasNext()){
			RolePermission effective = permissionIterator.next();
			boolean isBestMatch = ObjectUtil.equals(effective.getControllerPathElementName(),firstPermission.getControllerPathElementName()) 
					&& ObjectUtil.equals(effective.getActionPathElementName(),firstPermission.getActionPathElementName());
			
			if (isBestMatch){
				if (effective.isAllowed()){
					return;
				}
			}else {
				break;
			}
		}
		
	
	
		throw new AccessDeniedException();

		
	}

}
