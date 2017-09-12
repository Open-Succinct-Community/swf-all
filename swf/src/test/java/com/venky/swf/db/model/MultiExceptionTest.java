package com.venky.swf.db.model;

import org.junit.Test;

import com.venky.core.util.MultiException;

public class MultiExceptionTest {

	@Test
	public void test() {
		MultiException ex = new MultiException();
		ex.add(new RuntimeException("Rating"));
		System.out.println(ex.getMessage());
	}

}
