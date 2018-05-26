package com.venky.swf.plugins.collab.db.model.user;

import java.util.List;

import com.venky.swf.db.annotations.column.ui.HIDDEN;

public interface User extends com.venky.swf.plugins.security.db.model.User{
	@Deprecated
	@HIDDEN
	public Long getCompanyId();
	public void setCompanyId(Long companyId);
	
	public List<UserCompany> getUserCompanies();
}
