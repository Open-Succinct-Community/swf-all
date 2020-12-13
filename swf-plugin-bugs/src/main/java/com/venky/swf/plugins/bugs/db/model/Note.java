package com.venky.swf.plugins.bugs.db.model;

import java.io.InputStream;
import java.io.Reader;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;

@ORDER_BY("UPDATED_AT DESC")
@EXPORTABLE(false)
public interface Note extends Model{
	@PROTECTION(Kind.NON_EDITABLE)
	@IS_NULLABLE(false)
	public Long getIssueId();
	public void setIssueId(Long id);
	public Issue getIssue();
	
	@Index
	public Reader getNotes();
	public void setNotes(Reader notes);

	@IS_VIRTUAL
	public String getAttachmentUrl();
	public void setAttachmentUrl(String url);

	@HIDDEN
	public InputStream getAttachment();
	public void setAttachment(InputStream content);
	
	@HIDDEN
	public String getAttachmentContentName();
	public void setAttachmentContentName(String name);
	
	@HIDDEN
	public String getAttachmentContentType();
	public void setAttachmentContentType(String contentType);

	@HIDDEN
	@COLUMN_DEF(StandardDefault.ZERO)
	public int getAttachmentContentSize();
	public void setAttachmentContentSize(int size);
	 
}
