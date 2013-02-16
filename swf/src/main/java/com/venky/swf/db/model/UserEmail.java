package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface UserEmail extends Model{
	@PARTICIPANT
	@UNIQUE_KEY
	public int getUserId();
	public void setUserId(int id);
	public User getUser();
	
	@UNIQUE_KEY
	public void setEmail(String email);
	public String getEmail();
}
