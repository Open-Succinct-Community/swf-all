package com.venky.swf.test.db.model;

import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.model.Model;

public interface Sample extends Model{
	@Index
	public String getName();
	public void setName(String name);
}
