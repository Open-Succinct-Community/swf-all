package com.venky.swf.plugins.lucene.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;

@EXPORTABLE(false)
public interface IndexDirectory extends Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String tableName);
	
	public List<IndexFile> getIndexFiles();
}
