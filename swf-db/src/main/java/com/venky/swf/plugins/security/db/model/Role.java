package com.venky.swf.plugins.security.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@HAS_DESCRIPTION_FIELD
@CONFIGURATION
@MENU("Admin")
public interface Role extends Model{
	@IS_NULLABLE(false)
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
}
