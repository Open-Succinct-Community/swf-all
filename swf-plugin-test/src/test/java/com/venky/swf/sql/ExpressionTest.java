package com.venky.swf.sql;

import static org.junit.Assert.*;

import org.junit.Test;

import com.venky.swf.routing.Router;

public class ExpressionTest {

	@Test
	public void test() {
		Router.instance().setLoader(getClass().getClassLoader());
		System.out.println(Expression.createExpression("A", Operator.IN, new Object[]{"A","1",1, null}).getRealSQL());
		System.out.println(Expression.createExpression("A", Operator.IN,new Object[]{}).getRealSQL());
		System.out.println(Expression.createExpression("A", Operator.IN).getRealSQL());
		System.out.println(Expression.createExpression("A", Operator.IN,new Object[]{null}).getRealSQL());
	}

}
