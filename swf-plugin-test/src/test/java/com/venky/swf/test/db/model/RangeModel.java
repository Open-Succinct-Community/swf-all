package com.venky.swf.test.db.model;

import com.venky.swf.db.annotations.column.validations.IntegerRange;
import com.venky.swf.db.annotations.column.validations.NumericRange;
import com.venky.swf.db.model.Model;

public interface RangeModel extends Model{
	@NumericRange(min=0,max=1)
	public Double getX();
	public void setX(Double dX);
	
	@IntegerRange(min=0,max=3)
	public Integer getY();
	public void setY(Integer iY);
}
