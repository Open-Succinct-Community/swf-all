package com.venky.swf.plugins.collab.db.model.participants.admin;



import java.sql.Date;
import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.model.Model;

public interface Company extends Model{
	@COLUMN_NAME("ID")
	@PROTECTION
	@HIDDEN
	@HOUSEKEEPING
	@PARTICIPANT
	public int getSelfCompanyId();
	public void setSelfCompanyId(int id);
	
	@IS_VIRTUAL
	public Company getSelfCompany();
	
	@IS_NULLABLE(false)
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);

	public Date getDateOfIncorporation();
	public void setDateOfIncorporation(Date date);
	
	public List<Facility> getFacilities();
	
	@PARTICIPANT
	public Integer getCreatorUserId();
}
