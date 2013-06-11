package com.venky.swf.plugins.wiki.db.model;

import java.io.InputStream;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;

@EXPORTABLE(false)
public interface Attachment extends Model{
	public InputStream getAttachment();
	public void setAttachment(InputStream attachment);

	@PROTECTION(Kind.NON_EDITABLE)
	@UNIQUE_KEY
	public String getAttachmentContentName();
	public void setAttachmentContentName(String name);

	@PROTECTION(Kind.NON_EDITABLE)
	public String getAttachmentContentType();
	public void setAttachmentContentType(String contentType);
	
	@PROTECTION(Kind.NON_EDITABLE)
	public int getAttachmentContentSize();
	public void setAttachmentContentSize(int size);

}
