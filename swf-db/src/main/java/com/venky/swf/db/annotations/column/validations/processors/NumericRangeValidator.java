package com.venky.swf.db.annotations.column.validations.processors;

import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.validations.NumericRange;

public class NumericRangeValidator extends FieldValidator<NumericRange> {

	public NumericRangeValidator(String pool) {
		super(pool);
	}

	@Override
	public boolean validate(NumericRange annotation,String humanizedFieldName,  String value,
			MultiException fieldException) {
		if (ObjectUtil.isVoid(value)){
			return true;
		}
		Double dValue = (Double) Database.getJdbcTypeHelper(getPool()).getTypeRef(Double.class).getTypeConverter().valueOf(value);
		boolean valid = (annotation.min() <= dValue && annotation.max() >= dValue);
		if (!valid){
			 fieldException.add(new FieldValidationException(humanizedFieldName + " must be between:(" + annotation.min() + "," + annotation.max() + ")"));
		}
		return valid;

	}

}
