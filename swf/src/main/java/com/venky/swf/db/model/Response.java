package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;

@IS_VIRTUAL
public interface Response extends Model{
	public void setStatus(String status);
	public String getStatus();
	
	public void setError(String error);
	public String getError();
}
