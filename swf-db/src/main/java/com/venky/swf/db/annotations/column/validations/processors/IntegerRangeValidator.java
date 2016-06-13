package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.validations.IntegerRange;
import com.venky.swf.exceptions.MultiException;

public class IntegerRangeValidator extends FieldValidator<IntegerRange>{

	public IntegerRangeValidator(String pool) {
		super(pool);
	}

	@Override
	public boolean validate(IntegerRange annotation, String humanizedFieldName, String value,
			MultiException fieldException) {
		if (ObjectUtil.isVoid(value)){
			return true;
		}
		Integer iValue = (Integer) Database.getJdbcTypeHelper(getPool()).getTypeRef(Integer.class).getTypeConverter().valueOf(value);
		boolean valid = (annotation.min() <= iValue && annotation.max() >= iValue);
		if (!valid){
			 fieldException.add(new FieldValidationException(humanizedFieldName + " must be between (" + annotation.min() + "," + annotation.max() + ")"));
		}
		return valid;
	}

}
