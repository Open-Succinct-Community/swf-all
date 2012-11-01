package com.venky.swf.plugins.collab.db.model.participants.admin;
import java.util.List;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.model.UNIQUE_KEY;
import com.venky.swf.db.model.Model;


public interface Facility extends Address, Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);

	@PARTICIPANT
	@UNIQUE_KEY
	@IS_NULLABLE(false)
	public Integer getCompanyId();
	public void setCompanyId(Integer companyId);
	public Company getCompany();

	public List<FacilityUser> getFacilityUsers();
}
