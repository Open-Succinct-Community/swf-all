package com.venky.swf.plugins.background.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;
@IS_VIRTUAL
public interface Trigger extends Model{
	public String getAgentName();
	public void setAgentName(String name);
}
