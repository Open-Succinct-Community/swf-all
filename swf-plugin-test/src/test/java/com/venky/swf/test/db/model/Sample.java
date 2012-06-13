package com.venky.swf.test.db.model;

import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.lucene.db.annotations.Index;

public interface Sample extends Model{
	@Index
	public String getName();
	public void setName(String name);
}
