package com.venky.swf.plugins.security.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;

public interface User extends Model{
	@HIDDEN
	@CONNECTED_VIA("USER_ID")
	public List<UserRole> getUserRoles();
}
