package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;

public interface UserFacility extends Model {
	@PARTICIPANT
	@UNIQUE_KEY
	public long getUserId();
	public void setUserId(long id);
	public User getUser();
	
	@PARTICIPANT
	@UNIQUE_KEY
	public long getFacilityId();
	public void setFacilityId(long id);
	public Facility getFacility();
}
