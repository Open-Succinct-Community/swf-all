package com.venky.swf.plugins.security.extensions;

import static com.venky.core.log.TimerStatistics.Timer.startTimer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.log.TimerStatistics.Timer;
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
import com.venky.swf.path.Path;
import com.venky.swf.plugins.security.db.model.RolePermission;
import com.venky.swf.plugins.security.db.model.UserRole;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.parser.SQLExpressionParser;
import com.venky.swf.sql.parser.XMLExpressionParser;

public class ParticipantControllerAccessExtension implements Extension{
	private static ParticipantControllerAccessExtension instance = null;  
	static {
		instance = new ParticipantControllerAccessExtension();
		Registry.instance().registerExtension(Path.ALLOW_CONTROLLER_ACTION, instance);
		Registry.instance().registerExtension(RolePermission.class.getSimpleName() + ".after.save"   , instance.permissionCacheBuster);
		Registry.instance().registerExtension(RolePermission.class.getSimpleName() + ".after.destroy", instance.permissionCacheBuster);
		Registry.instance().registerExtension(UserRole.class.getSimpleName() + ".after.save"   , instance.permissionCacheBuster);
		Registry.instance().registerExtension(UserRole.class.getSimpleName() + ".after.destroy", instance.permissionCacheBuster);	}
	
	
	private class PermissionCacheBuster implements Extension {
		@Override
		public void invoke(Object... context) {
			synchronized (permissionCache) {
				for (String key :permissionCache.keySet()){
					permissionCache.get(key).clear();
				}
				permissionCache.clear();
			}
		}
	}
	
	private PermissionCacheBuster permissionCacheBuster = new PermissionCacheBuster();
	
	public void invoke(Object... context) {
		Timer timer = startTimer("Participant Controller Action invoke",Config.instance().isTimerAdditive());
		try {
			_invoke(context);
		}finally {
			timer.stop();
		}
	}

	private boolean isControllerActionAccessibleAtAll(final User user, final String controllerPathElementName, final String actionPathElementName,final Path path){
		String transactionKey = getClass().getName()+".isControllerActionAccessibleAtAll";
		Cache<String,Cache<String,Boolean>> cache = Database.getInstance().getCurrentTransaction().getAttribute(transactionKey);
		
		if (cache == null){
			cache = new Cache<String, Cache<String,Boolean>>() {
				private static final long serialVersionUID = 998528782452357935L;

				@Override
				protected Cache<String, Boolean> getValue(final String controllerPathElementName) {
					return new Cache<String, Boolean>() {
						private static final long serialVersionUID = 1897514771224474367L;

						@Override
						protected Boolean getValue(final String actionPathElementName) {
							return isControllerActionAccessible(user,controllerPathElementName, actionPathElementName, null, path);
						}
					};
				}
			}; 
			Database.getInstance().getCurrentTransaction().setAttribute(transactionKey,cache);
		}
		
		return cache.get(controllerPathElementName).get(actionPathElementName);
		
		
	}
	
	private boolean isControllerActionAccessible(final User user, final String controllerPathElementName, final String actionPathElementName, 
			final String parameterValue, 
			Path path){
		
		Timer timer = startTimer("Check If Action is Secured",Config.instance().isTimerAdditive());
		boolean securedAction = path.isActionSecure(actionPathElementName);
		timer.stop();
		
		if (!securedAction){
			return true;
		}else if (user == null){
			return false;
		}
		
		Class<? extends Model> modelClass  = null;
		Set<String> participantingRoles = new HashSet<String>();
		Model selectedModel = null;

		Table<? extends Model> possibleTable = Path.getTable(controllerPathElementName);
		if ( possibleTable != null ){
			modelClass = possibleTable.getModelClass();
		}
		Timer gettingParticipatingRoles = startTimer("Getting participating Roles",Config.instance().isTimerAdditive());
		if (modelClass != null ){
			Timer t = startTimer("Getting model Reflector", Config.instance().isTimerAdditive());
			ModelReflector<? extends Model> ref = ModelReflector.instance(modelClass);
			t.stop();
			
			if (parameterValue != null){
				t = startTimer("Getting Participating Roles when parameter != null", Config.instance().isTimerAdditive());
				try {
					int id = Integer.valueOf(parameterValue);
					selectedModel = possibleTable.get(id);
					if (selectedModel != null){
						participantingRoles = selectedModel.getParticipatingRoles(user);
					}
				}catch (NumberFormatException ex){
					//
				}catch (IllegalArgumentException ex) {
					throw new RuntimeException(ex);
				}finally {
					t.stop();
				}
			}else {
				t = startTimer("Getting Participating Roles when parameter == null", Config.instance().isTimerAdditive());
				participantingRoles = ref.getParticipatableRoles() ;
				t.stop();
			}
		}
		gettingParticipatingRoles.stop();
		
		Timer preparingPermissionQuery = startTimer("Preparing Permission query",Config.instance().isTimerAdditive());

		Expression permissionQueryWhere = new Expression(Conjunction.AND);

		Expression participationWhere = new Expression(Conjunction.OR);
		participationWhere.add(new Expression("participation",Operator.EQ));
		for (String participatingRole:participantingRoles){
			participationWhere.add(new Expression("participation",Operator.EQ,new BindVariable(participatingRole)));
		}
		permissionQueryWhere.add(participationWhere);
		
		boolean defaultController = false;
		if (ObjectUtil.isVoid(controllerPathElementName)){
			defaultController = true;
		}
		
		Expression controllerActionWhere = new Expression(Conjunction.OR);
		controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ))
														.add(new Expression("action_path_element_name",Operator.EQ)));
		
		if (defaultController){
			controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ))
														.add(new Expression("action_path_element_name",Operator.EQ)));
		}else {
			controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ,controllerPathElementName))
					.add(new Expression("action_path_element_name",Operator.EQ)));
		}
		if (defaultController){
			controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ))
					.add(new Expression("action_path_element_name",Operator.EQ,new BindVariable(actionPathElementName))));
		}else {
			controllerActionWhere.add(new Expression(Conjunction.AND).add(new Expression("controller_path_element_name",Operator.EQ,controllerPathElementName))
					.add(new Expression("action_path_element_name",Operator.EQ,new BindVariable(actionPathElementName))));
		}
		permissionQueryWhere.add(controllerActionWhere);

		preparingPermissionQuery.stop(); 
		
		Timer selectingUserRole = startTimer("Selecting user Roles",Config.instance().isTimerAdditive());
		Select userRoleQuery = new Select().from(UserRole.class).where(new Expression("user_id",Operator.EQ,new BindVariable(user.getId())));
		List<UserRole> userRoles = userRoleQuery.execute(UserRole.class);
		selectingUserRole.stop();
		
		Timer preparingRoleWhere = startTimer("Preparing role Where clause",Config.instance().isTimerAdditive());
		List<Integer> userRoleIds = new ArrayList<Integer>();
		Expression roleWhere = new Expression(Conjunction.OR);
		roleWhere.add(new Expression("role_id",Operator.EQ));
		if (!userRoles.isEmpty()){
			for (UserRole ur:userRoles){
				userRoleIds.add(ur.getRoleId());
			}
			roleWhere.add(new Expression("role_id",Operator.IN,userRoleIds.toArray()));
		}
		preparingRoleWhere.stop();
		
		permissionQueryWhere.add(roleWhere);
		
		
		Timer selectingRolePermissions = startTimer("Selecting from role permissions",Config.instance().isTimerAdditive());
		
		Select permissionQuery = new Select().from(RolePermission.class);
		permissionQuery.where(permissionQueryWhere);
		List<RolePermission> permissions = permissionQuery.execute();
		
		selectingRolePermissions.stop();
		
		if (selectedModel != null){ 
			Timer removingPermissionRecords = startTimer("Remove permission records based on condition.",Config.instance().isTimerAdditive());
			for (Iterator<RolePermission> permissionIterator = permissions.iterator(); permissionIterator.hasNext() ; ){
				RolePermission permission = permissionIterator.next();
				Reader condition = permission.getConditionText();
				String sCondition = (condition == null ? null : StringUtil.read(condition));
				if (!ObjectUtil.isVoid(sCondition)){
					Expression expression = new SQLExpressionParser(modelClass).parse(sCondition);
					if (expression == null ){
						expression = new XMLExpressionParser(modelClass).parse(sCondition);
					}
					if (!expression.eval(selectedModel)) {
						permissionIterator.remove();
					}
				}
			}
			removingPermissionRecords.stop();
		}

		if (permissions.isEmpty()){
			return true ;
		}
		
		return permissionCache.isAllowed(permissions, userRoleIds);
	}
	
	private PermissionCache permissionCache = new PermissionCache();
	
	private static class PermissionCache extends Cache<String,Cache<String,Boolean>> {
		
		private static final long serialVersionUID = 8076958083615092776L;

		public boolean isAllowed(List<RolePermission> permissions, List<Integer> userRoleIds){
			List<Integer> permissionIds = DataSecurityFilter.getIds(permissions);
			List<Integer> copyuserRoleIds =  new ArrayList<Integer>(userRoleIds);
			Collections.sort(permissionIds);
			Collections.sort(copyuserRoleIds);
			String userRolesKey = copyuserRoleIds.toString();
			String permissionsKey = permissionIds.toString();
			Boolean value = get(userRolesKey).get(permissionsKey);
			if  (value == null){
				value = calculatePermission(permissions, userRoleIds);
				get(userRolesKey).put(permissionsKey, value);
			}
			return value;
		}
		@Override
		protected Cache<String, Boolean> getValue(String k) {
			return new Cache<String, Boolean>() {
				private static final long serialVersionUID = -6669779570540556969L;

				@Override
				protected Boolean getValue(String k) {
					return null;
				}
			};
		}
		
		private boolean calculatePermission(List<RolePermission> permissions, List<Integer> userRoleIds){
			Timer sortingPermissions = startTimer("sorting permissions", Config.instance().isTimerAdditive());
			Collections.sort(permissions, rolepermissionComparator);
			sortingPermissions.stop();
			
			//permissions,userRoleIds, 

			Timer permissionsChecking = startTimer("Checking Permissions for being allowed",Config.instance().isTimerAdditive());
			RolePermission firstPermission = permissions.get(0);
			RolePermission currentPermissionGroup = firstPermission;

			Iterator<RolePermission> permissionIterator = permissions.iterator();
			while (permissionIterator.hasNext()){
				RolePermission effective = permissionIterator.next();
				if (permissionGroupComparator.compare(currentPermissionGroup,effective) < 0){
					if (currentPermissionGroup.getRoleId() != null ){
						userRoleIds.remove(effective.getRoleId());
					}else {
						break;
					}
					currentPermissionGroup = effective;
				}
				if (effective.getRoleId() != null && !userRoleIds.contains(effective.getRoleId())){
					//Disallowed at more granular level for this role. So Ignore this record.
					continue;
				}
				
				if (effective.isAllowed()){
					if (effective.getRoleId() != null || firstPermission.getRoleId() == null){
						return true;
					}else if (!userRoleIds.isEmpty() ){
						//First role not null but effective.role is null.
						//If User has atleast one more role that is not configured as disallowed then allowed.
						return true;
					}else {
						//Role level dissallowed will override.
						break;
					}
				}
			}
			permissionsChecking.stop();

			return false;

		}
		private Comparator<RolePermission> permissionGroupComparator = new Comparator<RolePermission>() {
			@Override
			public int compare(RolePermission o1, RolePermission o2) {
				int ret = 0;
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getControllerPathElementName()).compareTo(StringUtil.valueOf(o1.getControllerPathElementName()));
				}
				if (ret == 0){
					ret = StringUtil.valueOf(o2.getActionPathElementName()).compareTo(StringUtil.valueOf(o1.getActionPathElementName()));	
				}
				if (ret == 0 && o1.getRoleId() != null && o2.getRoleId() != null){
					ret = o1.getRoleId().compareTo(o2.getRoleId());
				}
				return ret;
			}
			
		};
		private Comparator<RolePermission> rolepermissionComparator = new Comparator<RolePermission>() {

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
					ret = permissionGroupComparator.compare(o1, o2);
				}
				if (ret == 0) {
					ret = StringUtil.valueOf(o2.getParticipation()).compareTo(StringUtil.valueOf(o1.getParticipation()));
				}
				return ret;
			}
			
		};

	}
	
	
	public void _invoke(Object... context) {
		User user = (User)context[0];
		if (user != null && user.isAdmin()){
			return;
		}
		String controllerPathElementName = (String)context[1];
		String actionPathElementName = (String)context[2];
		String parameterValue = (String)context[3];
		Path tmpPath = (Path)context[4];
		
		if (tmpPath == null){
			Timer constructPath = startTimer("Create Path",Config.instance().isTimerAdditive());
			tmpPath = new Path("/"+controllerPathElementName+"/"+actionPathElementName + (parameterValue == null ? "" : "/"+parameterValue));
			constructPath.stop();
		}
		if (!isControllerActionAccessibleAtAll(user, controllerPathElementName, actionPathElementName, tmpPath)){
			//This is a cached Check.
			throw new AccessDeniedException(tmpPath.getTarget());
		}
		if (!isControllerActionAccessible(user, controllerPathElementName, actionPathElementName, parameterValue, tmpPath)){
			throw new AccessDeniedException(tmpPath.getTarget());
		}
	}
	
}
