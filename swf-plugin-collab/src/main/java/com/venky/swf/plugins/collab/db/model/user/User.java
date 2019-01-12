package com.venky.swf.plugins.collab.db.model.user;

import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface User extends com.venky.swf.plugins.security.db.model.User , Address {

	@PARTICIPANT
	@HIDDEN
	@IS_VIRTUAL
	public Long getCompanyId();
	public void setCompanyId(Long companyId);
	public Company getCompany();

	public List<UserCompany> getUserCompanies();
}
