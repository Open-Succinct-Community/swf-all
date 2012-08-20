package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.MENU;

public interface UserEmail extends Model{
	@PARTICIPANT
	public int getUserId();
	public void setUserId(int id);
	public User getUser();
	
	public void setEmail(String email);
	public String getEmail();
}
