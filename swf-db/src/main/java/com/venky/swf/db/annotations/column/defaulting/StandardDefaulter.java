package com.venky.swf.db.annotations.column.defaulting;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import com.venky.core.date.DateUtils;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.model.User;

public class StandardDefaulter {
	public static Object getDefaultValue(COLUMN_DEF columnDef){
		return getDefaultValue(columnDef.value(), columnDef.args());
	}
	public static Object getDefaultValue(StandardDefault defaultKey){
		return getDefaultValue(defaultKey,"");
	}
	public static Object getDefaultValue(StandardDefault defaultKey,String args){
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
			case CURRENT_USER:
				User user = Database.getInstance().getCurrentUser();
				ret = user == null ? 1 : user.getId();
				break;
			case CURRENT_DATE:
				{
					Date date = new Date(System.currentTimeMillis());
					String format = args;
					if (!ObjectUtil.isVoid(format)){
						ret = DateUtils.getTimestampStr(date, TimeZone.getDefault(), format);
					}else {
						ret = new Date(DateUtils.getStartOfDay(date).getTime());
					}
				}
				break;
			case CURRENT_TIMESTAMP:
				{
					Timestamp ts = new Timestamp(System.currentTimeMillis());
					String format = args;
					if (!ObjectUtil.isVoid(format)){
						ret = DateUtils.getTimestampStr(ts, TimeZone.getDefault(), format);
					}else {
						ret = ts;
					}
				}
				break;
			case CURRENT_DAY_OF_MONTH:
				ret = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
				break;
			case CURRENT_MONTH:
				ret = Calendar.getInstance().get(Calendar.MONTH);
				break;
			case CURRENT_YEAR:
				ret = Calendar.getInstance().get(Calendar.YEAR);
				break;	
			case BOOLEAN_FALSE: 
				ret = false; 
				break;
			case BOOLEAN_TRUE:
				ret = true; 
				break;
			case SOME_VALUE:
				ret = args;
				break;
			case NONE:
				break;
		}
		return ret;
	}
	
}
