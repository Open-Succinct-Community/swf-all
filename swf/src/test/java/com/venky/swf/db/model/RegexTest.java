package com.venky.swf.db.model;

import java.util.regex.Pattern;

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
	@Test
	public void testFractionRegex(){
		Pattern p = Pattern.compile("^*(([0]*)|(0.[0-9]*)|(1.[0]*)|1)$") ;
		
		Assert.assertTrue(p.matcher("0").matches());
		Assert.assertFalse(p.matcher("-0").matches());
		Assert.assertFalse(p.matcher("+0").matches());
		Assert.assertTrue(p.matcher("0.1").matches());
		Assert.assertTrue(p.matcher("0.9").matches());

		Assert.assertTrue(p.matcher("0.01").matches());
		Assert.assertTrue(p.matcher("0.099").matches());
		Assert.assertTrue(p.matcher("0.11").matches());
		Assert.assertTrue(p.matcher("0.99").matches());
		Assert.assertTrue(p.matcher("1.0").matches());
		Assert.assertTrue(p.matcher("1.").matches());
		Assert.assertTrue(p.matcher("1").matches());
	}
	
	@Test
	public void testSheetName(){
		Pattern p = Pattern.compile("[^\\\\/?\\[\\]*]*");
		
		Assert.assertFalse(p.matcher("A/B").matches());
		Assert.assertFalse(p.matcher("A\\B").matches());
		Assert.assertFalse(p.matcher("X?Y").matches());
		Assert.assertFalse(p.matcher("AZ*D").matches());
		Assert.assertFalse(p.matcher("[AVB").matches());
		Assert.assertFalse(p.matcher("AVB]").matches());
		Assert.assertTrue(p.matcher("AVB").matches());
		
	}
}
