package com.venky.swf.db.annotations.model.validations;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.validations.UniqueKeyValidator.UniqueConstraintViolatedException;
import com.venky.swf.exceptions.MultiException;
import com.venky.swf.routing.Router;
import com.venky.swf.test.db.model.CommonCode;

public class UniqueKeyValidatorTest {
	@Before
	public void setUp(){ 
		Router.instance().setLoader(getClass().getClassLoader());
	}
	@Test
	public void test1() {
		CommonCode cc1 = Database.getTable(CommonCode.class).newRecord();
		CommonCode cc2 = Database.getTable(CommonCode.class).newRecord();
		cc1.setUpdaterUserId(1);
		cc2.setUpdaterUserId(1);
		cc1.save();
		cc2.save();
	}

	@Test
	public void test2() {
		CommonCode cc1 = Database.getTable(CommonCode.class).newRecord();
		CommonCode cc2 = Database.getTable(CommonCode.class).newRecord();
		cc1.setName("A");
		cc2.setName("A");
		cc1.save();
		try {
			cc2.save();
		}catch (Throwable ex){
			MultiException th = (MultiException)ExceptionUtil.getEmbeddedException(ex, MultiException.class);
			Assert.assertTrue(th != null);
			if (th != null){
				Assert.assertTrue(th.getContainedException(UniqueConstraintViolatedException.class) != null);
			}
		}
		
	}
}
