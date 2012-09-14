package com.venky.swf.sql;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.routing.Router;

public class CountTest {
	@BeforeClass
	public static void beforeAllTests(){
		Router.instance().setLoader(SelectTest.class.getClassLoader());
	}

	@Test
	public void test() {
		Assert.assertEquals(1,Database.getTable(User.class).recordCount());
	}
	
}
