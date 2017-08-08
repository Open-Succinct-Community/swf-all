package com.venky.swf.plugins.collab.db.model.participants.admin;
import java.util.List;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;


public interface Facility extends CompanySpecific, GeoLocation, Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	public String getAddressLine1();
	public void setAddressLine1(String line1);
	
	public String getAddressLine2();
	public void setAddressLine2(String line2);
	
	public String getAddressLine3();
	public void setAddressLine3(String line3);

	public String getAddressLine4();
	public void setAddressLine4(String line4);
	
	@PARTICIPANT("CITY")
	@OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.participants.admin.FacilityCitySelectionProcessor")
	public int getCityId();
	public void setCityId(int cityId);
	public City getCity();

	@PARTICIPANT("STATE")
	@OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.participants.admin.FacilityStateSelectionProcessor")
	public int getStateId();
	public void setStateId(int stateId);
	public State getState();
	
	@PARTICIPANT("COUNTRY")
	public int getCountryId();
	public void setCountryId(int countryId);
	public Country getCountry();
	

	@RegEx("[0-9]*")
	@COLUMN_SIZE(6)
	public String getPincode();
	public void setPincode(String pincode);
	
	public List<FacilityUser> getFacilityUsers();
}
