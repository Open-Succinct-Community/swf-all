package com.venky.swf.plugins.oauth.db.model;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTED;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;

public interface UserEmail extends Model{
	@PARTICIPANT
	public int getUserId();
	public void setUserId(int id);
	public User getUser();
	
	
	@PROTECTED
	@PARTICIPANT
	@COLUMN_NAME("USER_ID")
	@HIDDEN
	@HOUSEKEEPING
	public Integer getSelfUserId();
	public void setSelfUserId(Integer selfUserId);
	@IS_VIRTUAL
	public User getSelfUser();
	
	public void setEmail(String email);
	public String getEmail();
	
}
