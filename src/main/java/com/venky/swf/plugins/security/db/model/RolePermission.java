package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;


public interface RolePermission extends Model{
	public int getRoleId();
	public void setRoleId(int roleId);
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
	
	@COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
	public boolean isAllowed();
	public void setAllowed(boolean allowed);

	
}
