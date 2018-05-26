package com.venky.swf.plugins.security.db.model;

import java.io.Reader;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@CONFIGURATION
@MENU("Admin")
public interface RolePermission extends Model{
	@Index
	public Long getRoleId();
	public void setRoleId(Long roleId);
	public Role getRole();
	
	@IS_NULLABLE
	@Index
	public String getParticipation();
	public void setParticipation(String participation);
	
	@IS_NULLABLE
	@Index
	public String getControllerPathElementName();
	public void setControllerPathElementName(String controllerPathElementName);
	
	@IS_NULLABLE
	@Index
	public String getActionPathElementName();
	public void setActionPathElementName(String actionPathElementName);
	
	@CONTENT_TYPE(MimeType.TEXT_PLAIN)
	@IS_NULLABLE
	@Index
	public Reader getConditionText();
	public void setConditionText(Reader text);
	
	@COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
	public boolean isAllowed();
	public void setAllowed(boolean allowed);

	
}
