package com.venky.swf.db.annotations.column.defaulting;

public enum StandardDefault {
	NULL,
	ZERO,
	ONE,
	BOOLEAN_TRUE,
	BOOLEAN_FALSE,
	SOME_VALUE,
	CURRENT_USER() {
		public boolean isComputed(){
			return true;
		}
	},
	CURRENT_DATE(){
		public boolean isComputed(){
			return true;
		}
	},
	CURRENT_TIMESTAMP(){
		public boolean isComputed(){
			return true;
		}
	};
	
	
	public boolean isComputed(){
		return false;
	}
	
}
