package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

@CONFIGURATION
public interface UserRole extends Model {
	public int getUserId();
	public void setUserId(int userId);
	public User getUser();
	
	public int getRoleId();
	public void setRoleId(int roleId);
	public Role getRole();
}
