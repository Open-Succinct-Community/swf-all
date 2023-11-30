package com.venky.swf.plugins.lucene.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;


@EXPORTABLE(false)
public interface IndexDirectory extends Model{
	@UNIQUE_KEY
	public String getName();
	public void setName(String tableName);

	@Enumeration(enumClass = "com.venky.swf.plugins.background.core.CoreTask$Priority")
	@COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "LOW")
	@IS_NULLABLE(value = false)
	public String getPriority();
	public void setPriority(String priority);

	
	public List<IndexFile> getIndexFiles();
}
