package com.venky.swf.test.db.model.xml;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface State extends Model{
	@UNIQUE_KEY
	public Country getCountry();
	public long getCountryId();
	public void setCountryId(long id);
	
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
}
