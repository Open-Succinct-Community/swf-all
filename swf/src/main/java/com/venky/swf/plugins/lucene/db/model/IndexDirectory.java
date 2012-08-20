package com.venky.swf.plugins.lucene.db.model;

import java.util.List;

import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface IndexDirectory extends Model{
	public String getName();
	public void setName(String tableName);
	
	public List<IndexFile> getIndexFiles();
}
