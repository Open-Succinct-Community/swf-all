package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface City extends Model {
	@Index
	public String getName();
	public void setName(String name);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	@Index
	public int getStateId();
	public void setStateId(int id);
	public State getState();
}
