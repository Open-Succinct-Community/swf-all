package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;

import java.util.List;

public interface User extends com.venky.swf.plugins.security.db.model.User , Address, CompanyNonSpecific{

	@IS_VIRTUAL
	public boolean isStaff();


	@COLUMN_NAME("ID")
	@PROTECTION
	@PARTICIPANT
	@HIDDEN
	@HOUSEKEEPING
	public Long getSelfUserId();
	public void setSelfUserId(Long userId);
	@IS_VIRTUAL
	public User getSelfUser();

	public List<UserFacility> getUserFacilities();
	public List<UserPhone> getUserPhones();
}
