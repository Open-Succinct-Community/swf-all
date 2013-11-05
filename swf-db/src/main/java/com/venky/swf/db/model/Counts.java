package com.venky.swf.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;

@IS_VIRTUAL
public interface Counts extends Model{
	public int getCount();
	public void setCount(int count);
}
