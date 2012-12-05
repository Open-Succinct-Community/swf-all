package com.venky.swf.plugins.bugs.db.model;

import java.io.Reader;
import java.util.List;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.ORDER_BY;
import com.venky.swf.db.model.Model;

@ORDER_BY("UPDATED_AT DESC")
public interface Note extends Model{
	@PROTECTION(Kind.NON_EDITABLE)
	@IS_NULLABLE(false)
	public Integer getIssueId();
	public void setIssueId(Integer id);
	public Issue getIssue();
	
	@Index
	public Reader getNotes();
	public void setNotes(Reader notes);
	
	public List<Attachment> getAttachments();
	 
}
