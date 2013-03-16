package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;


public interface UserEmail extends  com.venky.swf.db.model.UserEmail, CompanySpecific{

	@IS_VIRTUAL
	@HIDDEN
	@PARTICIPANT(redundant=true)
	public Integer getCompanyId();
}
