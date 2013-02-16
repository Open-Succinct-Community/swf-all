package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface User extends com.venky.swf.db.model.User{
	@PARTICIPANT
	@IS_NULLABLE(true)
	public Integer getCompanyId();
	public void setCompanyId(Integer companyId);
	public Company getCompany();
}
