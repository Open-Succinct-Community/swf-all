package com.venky.swf.plugins.datamart.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.DBPOOL;
import com.venky.swf.db.model.Model;

@DBPOOL("datamart")
public interface EtlRestart extends Model {
	@UNIQUE_KEY
	public String getAgentName();
	public void setAgentName(String name);
	
	public String getRestartFieldName();
	public void setRestartFieldName(String name);
	
	public String getRestartFieldValue(); 
	public void setRestartFieldValue(String  value);
	
}
