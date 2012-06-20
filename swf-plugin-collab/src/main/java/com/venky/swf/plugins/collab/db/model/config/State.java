package com.venky.swf.plugins.collab.db.model.config;

import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;

@MENU_CONFIGURATION
@CONFIGURATION
public interface State extends Model{
	public String getName();
	public void setName(String name);
}
