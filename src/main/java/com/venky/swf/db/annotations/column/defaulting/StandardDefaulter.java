package com.venky.swf.db.annotations.column.defaulting;

import java.sql.Date;

public class StandardDefaulter {
	public static Object getDefaultValue(StandardDefault defaultKey){
		Object ret = null;
		switch (defaultKey){
			case NULL:
				ret = null;
				break;
			case STR_NONE:
				ret = "";
				break;
			case STR_BLANK:
				ret = " ";
				break;
			case ONE:
				ret = 1;
				break;
			case ZERO:
				ret = 0;
				break;
			case CURRENT_DATE:
				ret = new Date(System.currentTimeMillis());
				break;
			case BOOLEAN_FALSE: 
				ret = false; 
				break;
			case BOOLEAN_TRUE:
				ret = true; 
				break;
		}
		return ret;
	}
}
