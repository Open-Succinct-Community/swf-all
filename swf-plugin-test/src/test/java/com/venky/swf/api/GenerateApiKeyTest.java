package com.venky.swf.api;

import org.junit.BeforeClass;
import org.junit.Test;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.routing.Router;

public class GenerateApiKeyTest {

	@BeforeClass
	public static void classSetup(){
		Router.instance().setLoader(GenerateApiKeyTest.class.getClassLoader());
	}
	@Test
	public void test() {
		User venky = Database.getTable(User.class).newRecord();
		venky.setName("venky");
		venky.generateApiKey();
		venky.save();
		
		User menky = Database.getTable(User.class).newRecord();
		menky.setName("menky");
		menky.generateApiKey();
		menky.save();
		
		Database.getInstance().getCurrentTransaction().rollback(null);
		
	}

}
