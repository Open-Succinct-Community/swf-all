package com.venky.swf.plugins.background.db.model;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Timestamp;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.HOUSEKEEPING;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.background.core.Task;

@MENU("Admin")
public interface DelayedTask extends Task, Model, Comparable<DelayedTask>{
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
	
	public void execute();
	public static final String[] DEFAULT_ORDER_BY_COLUMNS = new String[] {"PRIORITY", "NUM_ATTEMPTS", "UPDATED_AT", "ID"}; //Field and column names are same.

	
	public static enum Priority {
		HIGH(-1),
		DEFAULT(0),
		LOW(1);
		private final int value; 

		Priority(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
		
	}
}
