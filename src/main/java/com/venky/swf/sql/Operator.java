package com.venky.swf.sql;

public enum Operator {
	EQ() {
		public String toString(){
			return "=";
		}
	},
	GT() {
		public String toString(){
			return ">";
		}
	},
	LT() {
		public String toString(){
			return "<";
		}
	},
	GE() {
		public String toString(){
			return ">=";
		}
	},
	LE() {
		public String toString(){
			return "<=";
		}
	},
	LK() {
		public String toString(){
			return "like";
		}
	},
	NE() {
		public String toString(){
			return "!=";
		}
	},

	IN(){
		public String toString(){
			return "in";
		}
		public boolean requiresParen(){
			return true;
		}
	};

	public boolean requiresParen(){
		return false;
	}
}
