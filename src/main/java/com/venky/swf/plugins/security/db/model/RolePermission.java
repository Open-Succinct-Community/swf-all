package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface RolePermission extends Model{
	public Integer getRoleId();
	public void setRoleId(Integer roleId);
	public Role getRole();
	
	@IS_NULLABLE
	public String getParticipation();
	public void setParticipation(String participation);
	
	@IS_NULLABLE
	public String getControllerPathElementName();
	public void setControllerPathElementName(String controllerPathElementName);
	
	@IS_NULLABLE
	public String getActionPathElementName();
	public void setActionPathElementName(String actionPathElementName);
	
	@COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
	public boolean isAllowed();
	public void setAllowed(boolean allowed);

	
}
