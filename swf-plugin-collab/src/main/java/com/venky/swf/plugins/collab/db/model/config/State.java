package com.venky.swf.plugins.collab.db.model.config;

import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
@CONFIGURATION
public interface State extends Model{
	@UNIQUE_KEY
	@Index
	public String getName();
	public void setName(String name);

	@UNIQUE_KEY
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.ONE)
	public Integer getCountryId();
	public void setCountryId(Integer iCountryId);
	public Country getCountry();
	
	public List<City> getCities();
}
