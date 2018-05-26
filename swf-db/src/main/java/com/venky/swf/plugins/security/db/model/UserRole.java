package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

@CONFIGURATION
@MENU("Admin")
public interface UserRole extends Model {
	@PARTICIPANT
	@UNIQUE_KEY
	@Index
	public long getUserId();
	public void setUserId(long userId);
	public User getUser();
	
	@UNIQUE_KEY
	@Index
	public long getRoleId();
	public void setRoleId(long roleId);
	public Role getRole();
}
