package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_COLUMN
@CONFIGURATION
public interface Role extends Model{
	public String getName();
	public void setName(String name);
}
