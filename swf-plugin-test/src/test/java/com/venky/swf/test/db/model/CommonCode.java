package com.venky.swf.test.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface CommonCode extends Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
}
