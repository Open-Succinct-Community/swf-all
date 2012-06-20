package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTED;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface User extends com.venky.swf.db.model.User{
	@PARTICIPANT
	public Integer getCompanyId();
	public void setCompanyId(Integer companyId);
	public Company getCompany();
	
	@COLUMN_NAME("ID")
	@PROTECTED
	@PARTICIPANT
	@HIDDEN
	@HOUSEKEEPING
	public Integer getSelfUserId();
	public void setSelfUserId(Integer userId);
	@IS_VIRTUAL
	public User getSelfUser();
	
}
