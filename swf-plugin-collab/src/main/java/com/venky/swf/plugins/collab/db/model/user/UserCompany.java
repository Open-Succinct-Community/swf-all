package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;


@ORDER_BY("ID")
public interface UserCompany extends Model, CompanySpecific {
	@PARTICIPANT
	@UNIQUE_KEY
	@HIDDEN
	public long getUserId();
	public void setUserId(long userId);
	public User getUser(); 
	
	@IS_NULLABLE(false)
	@Index
	@UNIQUE_KEY
	@PARTICIPANT("COMPANY")
	public Long getCompanyId();
	public void setCompanyId(Long id);
	public Company getCompany();

}
