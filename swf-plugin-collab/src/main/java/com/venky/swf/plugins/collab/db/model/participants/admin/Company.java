package com.venky.swf.plugins.collab.db.model.participants.admin;



import java.sql.Date;
import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTED;
import com.venky.swf.db.model.Model;

@MENU_ADMIN
public interface Company extends Model{
	@COLUMN_NAME("ID")
	@PROTECTED
	@HIDDEN
	@HOUSEKEEPING
	@PARTICIPANT
	public int getSelfCompanyId();
	public void setSelfCompanyId(int id);
	
	@IS_VIRTUAL
	public Company getSelfCompany();
	
	public String getName();
	public void setName(String name);

	public Date getDateOfIncorporation();
	public void setDateOfIncorporation(Date date);
	
	public List<Facility> getFacilities();
	
	@PARTICIPANT
	public Integer getCreatorUserId();
}