package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

import java.util.List;

public interface User extends com.venky.swf.plugins.templates.db.model.User, Address, CompanyNonSpecific{

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

	@CONNECTED_VIA("USER_ID")
	public List<UserFacility> getUserFacilities();

	@CONNECTED_VIA("USER_ID")
	public List<UserPhone> getUserPhones();


	@IS_VIRTUAL
	public List<Long> getCompanyIds();

	@IS_VIRTUAL
	public List<Company> getCompanies();
}
