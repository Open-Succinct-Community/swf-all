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
	    return getDefaultValue(defaultKey,args,TimeZone.getDefault());
    }
	public static Object getDefaultValue(StandardDefault defaultKey,String args, TimeZone tz){
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
					String format = args;
					if (!ObjectUtil.isVoid(format)){
						ret = DateUtils.getTimestampStr(new Date(System.currentTimeMillis()), tz, format);
					}else {
						ret = new Date(DateUtils.getStartOfDay(getNow(tz).getTime()));
					}
				}
				break;
			case CURRENT_TIMESTAMP:
				{
					String format = args;
					if (!ObjectUtil.isVoid(format)){
						ret = DateUtils.getTimestampStr(new Timestamp(System.currentTimeMillis()), tz, format);
					}else {
						ret = getNow(tz);
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

	public static Timestamp getNow(TimeZone tz) {
	    if (tz == null || tz.equals(TimeZone.getDefault())) {
	        return new Timestamp(System.currentTimeMillis());
        }
	    Calendar target = Calendar.getInstance(tz);
        target.setTimeInMillis(System.currentTimeMillis());

        Calendar sys = Calendar.getInstance(TimeZone.getDefault());
        sys.set(Calendar.YEAR,target.get(Calendar.YEAR));
        sys.set(Calendar.MONTH,target.get(Calendar.MONTH));
        sys.set(Calendar.DAY_OF_MONTH,target.get(Calendar.DAY_OF_MONTH));
        sys.set(Calendar.HOUR_OF_DAY,target.get(Calendar.HOUR_OF_DAY));
        sys.set(Calendar.MINUTE,target.get(Calendar.MINUTE));
        sys.set(Calendar.SECOND,target.get(Calendar.SECOND));
        sys.set(Calendar.MILLISECOND,target.get(Calendar.MILLISECOND));

        return new Timestamp(sys.getTimeInMillis());
    }
}
