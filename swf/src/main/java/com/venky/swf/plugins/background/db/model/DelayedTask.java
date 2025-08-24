package com.venky.swf.plugins.background.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.Task;

import java.io.InputStream;
import java.io.Reader;

@MENU("Tasks")
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

	public String getTaskClassName();
	public void setTaskClassName(String taskClassName);
	
	@IS_VIRTUAL
	public Task getContainedTask() ;
	
	@IS_VIRTUAL
	public Priority getTaskPriority();
	
	@IS_VIRTUAL
	public long getTaskId();
	
	
	public void execute();
	
	public static final String[] DEFAULT_ORDER_BY_COLUMNS = new String[] {"PRIORITY", "NUM_ATTEMPTS", "UPDATED_AT", "ID"}; //Field and column names are same.

	public boolean canExecuteRemotely() ;

	@IS_VIRTUAL
	public AsyncTaskManager getAsyncTaskManager();
	

	@IS_VIRTUAL
	public void onStart() ;

	@IS_VIRTUAL
	public void onSuccess() ;

	@IS_VIRTUAL
	public void onException(Throwable ex) ;


	@IS_VIRTUAL
	public void onComplete();

}
