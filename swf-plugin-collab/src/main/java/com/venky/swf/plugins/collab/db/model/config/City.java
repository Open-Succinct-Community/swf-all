package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface City extends Model {
	@Index
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	@Index
	@UNIQUE_KEY
	@IS_NULLABLE(false)
	public Integer getStateId();
	public void setStateId(Integer id);
	public State getState();
}
