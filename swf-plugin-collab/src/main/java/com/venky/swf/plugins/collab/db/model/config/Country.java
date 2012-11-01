package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface Country extends Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
}
