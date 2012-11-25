package com.venky.swf.test.db.model.xml;

import java.util.List;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface Country extends Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String name);
	
	public List<State> getStates();
}
