package com.venky.swf.db.annotations.model.validations;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.routing.Router;
import com.venky.swf.test.db.model.RangeModel;

public class RangeTest {
	@BeforeClass
	public static void setUp(){ 
		Router.instance().setLoader(RangeTest.class.getClassLoader());
	}
	@Test
	public void test(){
		RangeModel model = Database.getTable(RangeModel.class).newRecord();
		for (double d = -1.0 ; d< 0 ; d+= 0.1 ){
			try {
				model.setX(d);
				model.save();
				Assert.fail("Save should have failed");
			}catch (Exception e){
				Assert.assertTrue(ExceptionUtil.getRootCause(e).getMessage().startsWith("RangeModelField:X Value out of Range:(0.0,1.0)"));
			}
		}
		for (double d = 0.0 ; d <= 1.0 ; d+= 0.1 ){
			model.setX(d);
			model.save();
		}

		for (int y = -10 ; y < 10 ; y ++ ){
			try {
				model.setY(y);
				model.save();
				if (y < 0  || y > 3){
					Assert.fail("Save should have failed");	
				}
			}catch (Exception e){
				if (y < 0 || y > 3){
					Assert.assertTrue(ExceptionUtil.getRootCause(e).getMessage().startsWith("RangeModelField:Y Value out of Range:(0,3)"));
				}else {
					Assert.fail("Save should have passed");
				}
			}
		}
		for (double d = 1.1 ; d < 2.0 ; d+= 0.1 ){
			try {
				model.setX(d);
				model.save();
				Assert.fail("Save should have failed");
			}catch (Exception e){
				Assert.assertTrue(ExceptionUtil.getRootCause(e).getMessage().startsWith("RangeModelField:X Value out of Range:(0.0,1.0)"));
			}
		}
		
	}
}
