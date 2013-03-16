package com.venky.swf.plugins.collab.db.model.participants.admin;
import java.util.List;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;


public interface Facility extends Address, CompanySpecific, Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);

	@IS_NULLABLE(false)
	@UNIQUE_KEY
	public Integer getCompanyId();
	
	public List<FacilityUser> getFacilityUsers();
}
