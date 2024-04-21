package com.venky.swf.plugins.background.db.model;

import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@EXPORTABLE(false)
@MENU("Logs")
public interface AgentStatus extends Model{
	public String getName();
	public void setName(String name);
	
	public boolean isRunning();
	public void setRunning(boolean running);
}
