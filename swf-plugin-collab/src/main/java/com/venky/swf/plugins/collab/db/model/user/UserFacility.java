package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
public interface UserFacility extends Model {
	@PARTICIPANT
	@UNIQUE_KEY
	public int getUserId();
	public void setUserId(int id);
	public User getUser();
	
	@PARTICIPANT
	@UNIQUE_KEY
	public int getFacilityId();
	public void setFacilityId(int id);
	public Facility getFacility();
}
