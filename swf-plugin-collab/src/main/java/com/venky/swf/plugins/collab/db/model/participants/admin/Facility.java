package com.venky.swf.plugins.collab.db.model.participants.admin;
import java.util.List;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;

public interface Facility extends Address, Model{
	public String getName();
	public void setName(String name);
	
	@PARTICIPANT
	public int getCompanyId();
	public void setCompanyId(int companyId);
	public Company getCompany();

	public List<FacilityUser> getFacilityUsers();
}
