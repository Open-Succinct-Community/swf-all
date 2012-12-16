package com.venky.swf.db.model;

import org.junit.Test;

import com.venky.swf.exceptions.MultiException;

public class MultiExceptionTest {

	@Test
	public void test() {
		MultiException ex = new MultiException();
		ex.add(new RuntimeException("Rating"));
		System.out.println(ex.getMessage());
	}

}
