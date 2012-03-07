package com.venky.swf.sql;

public enum Conjunction {
	AND {
		public String toString(){
			return  "AND";
		}
	},
	OR {
		public String toString(){
			return  "OR";
		}
	},
}
