package com.venky.swf.plugins.background.db.model;

import java.io.InputStream;
import java.io.Reader;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.Task;

@MENU("Admin")
@EXPORTABLE(false)
public interface DelayedTask extends Task, Model {
	public InputStream getData();
	public void setData(InputStream stream);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getNumAttempts();
	public void setNumAttempts(int numAttempts);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getPriority();
	public void setPriority(int priority);
	
	public Reader getLastError();
	public void setLastError(Reader s);

	@IS_VIRTUAL
	public String getTaskClassName();
	
	
	@IS_VIRTUAL
	public Priority getTaskPriority();
	
	@IS_VIRTUAL
	public long getTaskId();
	
	
	public void execute();
	
	public static final String[] DEFAULT_ORDER_BY_COLUMNS = new String[] {"PRIORITY", "NUM_ATTEMPTS", "UPDATED_AT", "ID"}; //Field and column names are same.

	public boolean canExecuteRemotely() ;
}
