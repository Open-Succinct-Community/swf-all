package com.venky.swf.plugins.collab.db.model.participants.admin;
import java.util.List;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;


@CONFIGURATION
public interface Facility extends CompanySpecific, Address , Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	public List<UserFacility> getFacilityUsers();
}
