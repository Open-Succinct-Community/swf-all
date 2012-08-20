package com.venky.swf.plugins.background.db.model;

import java.io.InputStream;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.Task;

public interface DelayedTask extends Task, Model, Comparable{
	public InputStream getData();
	public void setData(InputStream stream);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getNumAttempts();
	public void setNumAttempts(int numAttempts);
	
	public String getLastError();
	public void setLastError(String s);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getPriority();
	public void setPriority(int priority);
	
	public void execute();
	public static final String[] DEFAULT_ORDER_BY_COLUMNS = new String[] {"PRIORITY", "NUM_ATTEMPTS", "UPDATED_AT", "ID"}; //Field and column names are same.

}
