package com.venky.swf.plugins.lucene.db.model;

import java.io.InputStream;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.annotations.model.TRANSACTION;
import com.venky.swf.db.model.Model;

@TRANSACTION
public interface IndexFile extends Model {
	
	@IS_NULLABLE(false)
	@COLUMN_DEF(StandardDefault.ZERO)
	public Integer getIndexDirectoryId();
	public void setIndexDirectoryId(Integer dirId);
	public IndexDirectory getIndexDirectory();
	
	public String getName();
	public void setName(String name);
	
	@COLUMN_DEF(StandardDefault.ZERO)
	public long getLength();
	public void setLength(long length);
	
	public InputStream getIndexContent();
	public void setIndexContent(InputStream reader);
}
