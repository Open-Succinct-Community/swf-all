package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;

import java.util.List;


public interface MasterFacilityCategory extends Model{

	@PARTICIPANT
	@IS_NULLABLE(false)
	@PROTECTION(Kind.NON_EDITABLE)
	@UNIQUE_KEY
	public long getCompanyId(); 
	public void setCompanyId(long id); 
	public Company getCompany(); 
	
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	
	
	public List<MasterFacilityCategoryValue> getAllowedValues();
}
