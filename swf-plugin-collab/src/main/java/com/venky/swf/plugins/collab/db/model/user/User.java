package com.venky.swf.plugins.collab.db.model.user;

import java.util.List;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.participants.admin.Address;

public interface User extends com.venky.swf.plugins.security.db.model.User , Address {
	@Deprecated
	@HIDDEN
	public Long getCompanyId();
	public void setCompanyId(Long companyId);
	
	public List<UserCompany> getUserCompanies();
}
