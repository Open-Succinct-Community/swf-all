package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;

public interface UserEmail extends Model{
	@PARTICIPANT
	@UNIQUE_KEY
	@Index
	public long getUserId();
	public void setUserId(long id);
	public User getUser();
	
	@UNIQUE_KEY
	@Index
	public String getEmail();
	public void setEmail(String email);
}
