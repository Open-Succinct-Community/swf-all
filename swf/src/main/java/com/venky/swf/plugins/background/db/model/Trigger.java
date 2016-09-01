package com.venky.swf.plugins.background.db.model;

import com.venky.swf.db.model.Model;

public interface Trigger extends Model{
	public String getAgentName();
	public void setAgentName(String name);
}
