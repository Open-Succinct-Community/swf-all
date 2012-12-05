package com.venky.swf.plugins.bugs.db.model;

import java.io.InputStream;

import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;

public interface Attachment extends Model{
	@HIDDEN	
	public int getNoteId();
	public void setNoteId(int noteId);
	public Note getNote();
	
	public InputStream getAttachment();
	public void setAttachment(InputStream content);
	
	@HIDDEN
	public String getAttachmentContentName();
	public void setAttachmentContentName(String name);
	
	@HIDDEN
	public String getAttachmentContentType();
	public void setAttachmentContentType(String contentType);

}
