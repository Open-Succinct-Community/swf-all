package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;

public interface CompanySpecific {
	@PARTICIPANT
	@IS_NULLABLE
	@Index
	public Integer getCompanyId();
	public void setCompanyId(Integer id);
	public Company getCompany();
	
	@PARTICIPANT(redundant=true)
	@PROTECTION
	@IS_VIRTUAL
	@HIDDEN
	public Integer getCompanyCreatorUserId();
	public User getCompanyCreatorUser();

}
