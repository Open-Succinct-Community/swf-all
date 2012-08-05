package com.venky.swf.db.annotations.column.validations.processors;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;

public class DateFormatValidator extends FieldValidator<COLUMN_DEF> {

	@Override
	public Class<COLUMN_DEF> getAnnotationClass() {
		return COLUMN_DEF.class;
	}

	@Override
	public boolean validate(COLUMN_DEF annotation, String value,
			StringBuilder message) {
		if (annotation.value() == StandardDefault.CURRENT_DATE || annotation.value() == StandardDefault.CURRENT_TIMESTAMP){
			String format = annotation.args();
			if (!ObjectUtil.isVoid(format)){
				try {
					new SimpleDateFormat(format).parse(value);
				} catch (ParseException e) {
					message.append("Input doesnot match format:").append(format);
					return false;
				}
			}
		}
		return true;
	}

}
