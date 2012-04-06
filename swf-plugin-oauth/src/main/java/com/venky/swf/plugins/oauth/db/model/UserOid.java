package com.venky.swf.plugins.oauth.db.model;

import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

public interface UserOid extends Model{
	public int getUserId();
	public void setUserId(int id);
	
	public User getUser();
	
	public void setEmail(String email);
	public String getEmail();
	
}
