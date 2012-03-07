package com.venky.swf.db.model;

import junit.framework.Assert;

import org.junit.Test;

public class RegexTest {

	@Test
	public void test() {
		String r = "^A.*$";//"^"+"%A%".replace("%", ".*") +"$";
		System.out.println(r);
		Assert.assertTrue("AllA".matches(r));
		Assert.assertFalse("allA".matches(r));
		//Patter
	}

}
