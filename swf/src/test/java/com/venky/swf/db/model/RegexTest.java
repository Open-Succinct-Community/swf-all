package com.venky.swf.db.model;

import junit.framework.Assert;

import org.junit.Test;

public class RegexTest {

	@Test
	public void test() {
		String r = "([0-1][0-9]|[2][0-3]):[0-5][0-9]";
		System.out.println(r);
		Assert.assertTrue("00:00".matches(r));
		Assert.assertTrue("09:00".matches(r));
		Assert.assertTrue("15:00".matches(r));
		Assert.assertTrue("19:00".matches(r));
		Assert.assertTrue("20:00".matches(r));
		Assert.assertTrue("22:59".matches(r));
		
		Assert.assertFalse("24:00".matches(r));
		Assert.assertTrue("22:09".matches(r));
		Assert.assertTrue("22:10".matches(r));
		Assert.assertTrue("22:19".matches(r));
		Assert.assertTrue("23:59".matches(r));
		Assert.assertFalse("23:60".matches(r));
		
		//Patter
	}

}
