package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;

public interface Address {
	
	public String getAddressLine1();
	public void setAddressLine1(String line1);
	
	public String getAddressLine2();
	public void setAddressLine2(String line2);
	
	public String getAddressLine3();
	public void setAddressLine3(String line3);

	public String getAddressLine4();
	public void setAddressLine4(String line4);
	
	public int getCityId();
	public void setCityId(int cityId);
	public City getCity();
	
	public int getStateId();
	public void setStateId(int stateId);
	public State getState();
	
	@RegEx("[0-9]*")
	@COLUMN_SIZE(6)
	public String getPincode();
	public void setPincode(String pincode);
	
	public int getCountryId();
	public void setCountryId(int countryId);
	public Country getCountry();
	
	public Float getLatitude(); 
	public void setLatitude(Float latitude);
	
	public Float getLongitude();
	public void setLongitude(Float longitude);
}
