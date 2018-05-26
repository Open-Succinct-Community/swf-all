package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;

public interface CompanySpecific {
	@PARTICIPANT
	@IS_NULLABLE(false)
	@Index
	@UNIQUE_KEY
	public Long getCompanyId();
	public void setCompanyId(Long id);
	public Company getCompany();
	
}
