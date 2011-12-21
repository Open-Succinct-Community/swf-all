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
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.security.db.model.RolePermission;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.routing.Path;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

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
						participantingRoles.add(referencedModelIdFieldName.substring(0, referencedModelIdFieldName.length()-3));
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

		Expression participationWhere = new Expression("OR");
		participationWhere.add(new Expression("participation",Operator.EQ));
		for (String participatingRole:participantingRoles){
			participationWhere.add(new Expression("participation",Operator.EQ,new BindVariable(participatingRole)));
		}
		
		Expression controllerActionWhere = new Expression("OR");
		controllerActionWhere.add(new Expression("controller_path_element_name",Operator.EQ));
		controllerActionWhere.add(new Expression("AND").add(new Expression("controller_path_element_name",Operator.EQ,new BindVariable(controllerPathElementName)))
														.add(new Expression("action_path_element_name",Operator.EQ)));
		controllerActionWhere.add(new Expression("AND").add(new Expression("controller_path_element_name",Operator.EQ,new BindVariable(controllerPathElementName)))
														.add(new Expression("action_path_element_name",Operator.EQ,new BindVariable(actionPathElementName))));
		
		Expression permissionQueryWhere = new Expression("AND");
		String userRole = Database.getInstance().getTable(UserRole.class).getTableName() ; 
		permissionQueryWhere.add(new Expression("role_id",Operator.IN, 
												new Select().from(userRole).where(
																					new Expression("user_id",Operator.EQ,new BindVariable(user.getId()))
																				)));
		permissionQueryWhere.add(participationWhere);
		permissionQueryWhere.add(controllerActionWhere);

		Select permissionQuery = new Select().from(Database.getInstance().getTable(RolePermission.class).getTableName());
		permissionQuery.where(permissionQueryWhere);

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
