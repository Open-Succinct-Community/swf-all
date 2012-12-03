package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
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
	public int getUserId();
	public void setUserId(int userId);
	public User getUser();
	
	@UNIQUE_KEY
	public int getRoleId();
	public void setRoleId(int roleId);
	public Role getRole();
}
