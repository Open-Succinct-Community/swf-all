package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;

@IS_VIRTUAL
public interface CompanyUser extends UserCompany{
	@HIDDEN(true)
	public Integer getCompanyId();
	
	@HIDDEN(false)
	public int getUserId();
	
	
}
