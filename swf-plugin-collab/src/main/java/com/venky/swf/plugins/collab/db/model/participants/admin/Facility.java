package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.calendar.db.model.WorkCalendar;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;

import java.util.List;


public interface Facility extends CompanySpecific, Address , Model{
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);
	
	public List<UserFacility> getFacilityUsers();


	public List<FacilityCategory> getFacilityCategories();
	@IS_NULLABLE
	public Long getWorkCalendarId();
	public void setWorkCalendarId(Long WorkCalendarId);
	public WorkCalendar getWorkCalendar();

}
