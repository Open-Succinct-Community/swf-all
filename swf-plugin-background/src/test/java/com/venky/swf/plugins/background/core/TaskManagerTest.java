package com.venky.swf.plugins.background.core;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.routing.Router;

public class TaskManagerTest {
	@Before
	public void setUp(){ 
		Router.instance().setLoader(getClass().getClassLoader());
		//Database.getTable(DelayedTask.class).truncate();
		//Database.getInstance().getCurrentTransaction().commit();
	}
	
	@Test
	public void testExecuteDelayed() throws SQLException {
		TaskManager.instance().executeDelayed(new HelloTask());
		Database.getInstance().getCurrentTransaction().commit();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		TaskManager.instance().shutdown();
	}
	
	public static class HelloTask implements Task {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3040311245585746198L;

		public void execute() {
			System.out.println("Hello World");
		}
		
	}

}
