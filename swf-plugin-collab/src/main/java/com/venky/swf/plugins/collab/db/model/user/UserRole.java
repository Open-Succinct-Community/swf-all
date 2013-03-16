package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;


public interface UserRole extends  com.venky.swf.plugins.security.db.model.UserRole, CompanySpecific{

	@IS_VIRTUAL
	@HIDDEN
	@PARTICIPANT(redundant=true)
	public Integer getCompanyId();
}
