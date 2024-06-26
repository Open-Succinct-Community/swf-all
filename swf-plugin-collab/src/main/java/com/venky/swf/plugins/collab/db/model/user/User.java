package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.plugins.collab.db.model.CompanyNonSpecific;
import com.venky.swf.plugins.collab.db.model.participants.Application;
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

	// Don't change the order of these methods.
	@CONNECTED_VIA(value = "CREATOR_ID")
	@HIDDEN
	public List<Company> getCreatedCompanies();

	@IS_VIRTUAL
	public List<Company> getCompanies();
	//Order changing comment ends.


	@CONNECTED_VIA(value = "CREATOR_ID",additional_join = "( COMPANY_ID IS NULL)")
	public List<Application> getApplications();
}
