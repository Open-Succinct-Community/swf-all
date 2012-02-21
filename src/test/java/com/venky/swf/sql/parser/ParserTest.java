package com.venky.swf.sql.parser;

import static com.venky.swf.sql.parser.SQLExpressionParser.digit;
import static com.venky.swf.sql.parser.SQLExpressionParser.openParen;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.venky.parse.Rule;
import com.venky.parse.composite.CharSequence;
import com.venky.parse.composite.Multiple;
import com.venky.parse.composite.Sequence;

public class ParserTest {

	@Test
	public void numberTest() {
		Rule number = new Multiple( digit() ); 
		boolean match  = number.match("A123456A",1);
		assertTrue(match);
		assertEquals(number.getMatch().getText(),"123456");
	}
	@Test
	public void charSeqTest() {
		Rule charSeq = new CharSequence("ABCDE");
		Rule clone = null;
		clone = charSeq.createClone();
		assertTrue(clone.match("ABCDEFGH", 0));
		assertEquals("ABCDE", clone.getMatch().getText());
		
		clone = charSeq.createClone();
		assertTrue(clone.match("ABCDE", 0));
		assertEquals("ABCDE", clone.getMatch().getText());

		clone = charSeq.createClone();
		assertTrue(!clone.match("ABCD", 0));

	}
	@Test
	public void ExactZeroMatchTest(){
		Rule r = new Multiple(new Sequence(openParen()),0,0);
		if (r.match("( A )")){
			assertEquals(0, r.getMatch().length());
		}
	}
	
}
