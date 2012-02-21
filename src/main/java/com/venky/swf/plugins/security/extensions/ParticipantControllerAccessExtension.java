package com.venky.swf.plugins.security.extensions;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.venky.core.string.StringUtil;
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
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.parser.SQLExpressionParser;
import com.venky.swf.sql.parser.XMLExpressionParser;

public class ParticipantControllerAccessExtension implements Extension{
	static {
		Registry.instance().registerExtension(Path.ALLOW_CONTROLLER_ACTION, new ParticipantControllerAccessExtension());
	}

	public void invoke(Object... context) {
		User user = (User)context[0];
		String controllerPathElementName = (String)context[1];
		String actionPathElementName = (String)context[2];
		String parameterValue = (String)context[3];
		Class<? extends Model> modelClass  = null;
		List<String> participantingRoles = new ArrayList<String>();
		Model selectedModel = null;

		Table possibleTable = Path.getTable(controllerPathElementName);
		if ( possibleTable != null ){
			modelClass = possibleTable.getModelClass();
		}
		if (modelClass != null && parameterValue != null){
			try {
				int id = Integer.valueOf(parameterValue);
				selectedModel = possibleTable.get(id);
				Map<String,List<Integer>> pOptions = user.getParticipationOptions(modelClass);
				ModelReflector<? extends Model> reflector = ModelReflector.instance(modelClass);
				for (String referencedModelIdFieldName :pOptions.keySet()){
					Integer referenceValue = (Integer)reflector.getFieldGetter(referencedModelIdFieldName).invoke(selectedModel);
					if (pOptions.get(referencedModelIdFieldName).contains(referenceValue)){
						participantingRoles.add(referencedModelIdFieldName.substring(0, referencedModelIdFieldName.length()-3));//Remove "_ID" from the end.
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

		Expression permissionQueryWhere = new Expression(Conjunction.AND);

		Expression participationWhere = new Expression(Conjunction.OR);
		participationWhere.add(new Expression("participation",Operator.EQ));
		for (String participatingRole:participantingRoles){
			participationWhere.add(new Expression("participation",Operator.EQ,new BindVariable(participatingRole)));
		}
		permissionQueryWhere.add(participationWhere);
		
		Expression controllerActionWhere = new Expression(Conjunction.OR);
		controllerActionWhere.add(new Expression("controller_path_element_name",Operator.EQ));
		controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ,new BindVariable(controllerPathElementName)))
														.add(new Expression("action_path_element_name",Operator.EQ)));
		controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ,new BindVariable(controllerPathElementName)))
														.add(new Expression("action_path_element_name",Operator.EQ,new BindVariable(actionPathElementName))));
		permissionQueryWhere.add(controllerActionWhere);

		String userRole = Database.getInstance().getTable(UserRole.class).getRealTableName() ;
		Select userRoleQuery = new Select("role_id").from(userRole).where(new Expression("user_id",Operator.EQ,new BindVariable(user.getId())));
		List<UserRole> userRoles = userRoleQuery.execute();
		List<Integer> userRoleIds = new ArrayList<Integer>();
		Expression roleWhere = new Expression(Conjunction.OR);
		roleWhere.add(new Expression("role_id",Operator.EQ));
		if (!userRoles.isEmpty()){
			List<BindVariable> role_ids = new ArrayList<BindVariable>();
			for (UserRole ur:userRoles){
				role_ids.add(new BindVariable(ur.getRoleId()));
				userRoleIds.add(ur.getRoleId());
			}
			roleWhere.add(new Expression("role_id",Operator.IN,role_ids.toArray(new BindVariable[]{})));
		}
		permissionQueryWhere.add(roleWhere);
		
		Select permissionQuery = new Select().from(RolePermission.class);
		permissionQuery.where(permissionQueryWhere);

		List<RolePermission> permissions = permissionQuery.execute();
		
		if (selectedModel != null){ 
			for (Iterator<RolePermission> permissionIterator = permissions.iterator(); permissionIterator.hasNext() ; ){
				RolePermission permission = permissionIterator.next();
				InputStream condition = null; //permission.getCondtion();
				if (condition != null ){
					String sCondition = StringUtil.read(condition);
					Expression expression = new SQLExpressionParser(modelClass).parse(sCondition);
					if (expression == null){
						expression = new XMLExpressionParser(modelClass).parse(sCondition);
					}
					if (!expression.eval(selectedModel)) {
						permissionIterator.remove();
					}
				}
			}
			
		}

		if (permissions.isEmpty()){
			return ;
		}
		
		Collections.sort(permissions, new Comparator<RolePermission>() {

			public int compare(RolePermission o1, RolePermission o2) {
				int ret =  0; 
				if (ret == 0){
					if (o1.getRoleId() == null && o2.getRoleId() != null){
						ret = 1;
					}else if (o2.getRoleId() == null && o1.getRoleId() != null){
						ret = -1;
					}else {
						ret = 0;
					}
				}
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getControllerPathElementName()).compareTo(StringUtil.valueOf(o1.getControllerPathElementName()));
				}
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getActionPathElementName()).compareTo(StringUtil.valueOf(o1.getActionPathElementName()));	
				}
				return ret;
			}
			
		});
		
		RolePermission firstPermission = permissions.get(0);
		Iterator<RolePermission> permissionIterator = permissions.iterator();
		while (permissionIterator.hasNext()){
			RolePermission effective = permissionIterator.next();
			if (effective.isAllowed()){
				if (effective.getRoleId() != null || firstPermission.getRoleId() == null){
					return;
				}else if (!userRoleIds.isEmpty()){
					//First role not null but effective.role is null.
					//If User has atleast one more role that is not configured as disallowed then allowed.
					return;
				}else {
					break;
				}
			}else if (effective.getRoleId() != null ){
				userRoleIds.remove(effective.getRoleId());
			}
		}
		throw new AccessDeniedException();
	}

}
