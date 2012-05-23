package com.venky.swf.db.annotations.column.defaulting;

import java.sql.Date;
import java.sql.Timestamp;

import com.venky.swf.db.annotations.column.COLUMN_DEF;

public class StandardDefaulter {
	public static Object getDefaultValue(COLUMN_DEF columnDef){
		return getDefaultValue(columnDef.value(), columnDef.someValue());
	}
	public static Object getDefaultValue(StandardDefault defaultKey,String someValue){
		Object ret = null;
		switch (defaultKey){
			case NULL:
				ret = null;
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
			case CURRENT_TIMESTAMP:
				ret = new Timestamp(System.currentTimeMillis());
				break;
			case BOOLEAN_FALSE: 
				ret = false; 
				break;
			case BOOLEAN_TRUE:
				ret = true; 
				break;
			case SOME_VALUE:
				ret = someValue;
				break;
		}
		return ret;
	}
	
}
