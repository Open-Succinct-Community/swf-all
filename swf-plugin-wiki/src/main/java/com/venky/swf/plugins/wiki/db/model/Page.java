package com.venky.swf.plugins.wiki.db.model;

import java.io.Reader;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.CLONING_PROTECT;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;

@HAS_DESCRIPTION_FIELD("TITLE")
public interface Page extends Model,CompanySpecific{
	@Index
	@WATERMARK("Untitled")
	@CLONING_PROTECT
	public String getTitle();
	public void setTitle(String title);
	
	@Index
	@WATERMARK("Tag words separated by space")
	@CLONING_PROTECT
	public String getTag();
	public void setTag(String tag);

	@CONTENT_TYPE(MimeType.TEXT_MARKDOWN)
	@Index
	@WATERMARK("Enter your markdown text here...")
	@CLONING_PROTECT
	public Reader getBody(); 
	public void setBody(Reader reader);
	
	
	@COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
	@CLONING_PROTECT
	public boolean isLandingPage();
	public void setLandingPage(boolean landingPage);
	
}
