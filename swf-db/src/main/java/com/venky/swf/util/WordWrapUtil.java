package com.venky.swf.util;

import java.util.StringTokenizer;

public class WordWrapUtil {

	public WordWrapUtil() {
	}

	public static int getNumRowsRequired(String sValue,int maxColumnLength){
		int vlen = 0; 
		int numRows = 1 ;
		StringTokenizer tok = new StringTokenizer(sValue," \n",true);
		
		while (tok.hasMoreTokens()){
			String token = tok.nextToken();
			int ctl = token.length() ;
			if (token.equals("\n")){
				vlen = (numRows * maxColumnLength);
			}
			if ( vlen + ctl >= numRows * maxColumnLength) {
				vlen = (numRows * maxColumnLength) + ctl;
				numRows += (Math.ceil(ctl * 1.0/maxColumnLength)) ;
			}else {
				vlen += ctl ;
			}
		}
		return numRows ;
	}
}
