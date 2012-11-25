package com.venky.swf.db.annotations.column.validations.processors;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.exceptions.MultiException;

public class DateFormatValidator extends FieldValidator<COLUMN_DEF> {

	@Override
	public boolean validate(COLUMN_DEF annotation, String value, MultiException ex){
		if (ObjectUtil.isVoid(value)){
			return true;
		}
		if (annotation.value() == StandardDefault.CURRENT_DATE || annotation.value() == StandardDefault.CURRENT_TIMESTAMP){
			String format = annotation.args();
			if (!ObjectUtil.isVoid(format)){
				try {
					new SimpleDateFormat(format).parse(value);
				} catch (ParseException e) {
					ex.add(new FieldValidationException("Input doesnot match format:" + format));
					return false;
				}
			}
		}
		return true;
	}
	

}
