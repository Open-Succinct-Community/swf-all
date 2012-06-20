package com.venky.swf.db;

import java.sql.SQLException;

import com.venky.swf.util._ICloseable;

public interface _IDatabase extends  _ICloseable{
	public void open(Object user);
	public _ITransaction getCurrentTransaction() throws SQLException;
	public interface _ITransaction {
		public void commit() throws SQLException;
		public void rollback() throws SQLException;
		public Object getAttribute(String string); 
	}
}
