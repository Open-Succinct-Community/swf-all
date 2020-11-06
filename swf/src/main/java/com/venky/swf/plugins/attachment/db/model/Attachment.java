package com.venky.swf.plugins.attachment.db.model;

import java.io.InputStream;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.EXPORTABLE;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;


@EXPORTABLE(false)
public interface Attachment extends Model{
	static Attachment find(String name) {
		Select select = new Select().from(Attachment.class);
		select.where(new Expression(select.getPool(),"ATTACHMENT_CONTENT_NAME", Operator.EQ,name));
		List<Attachment> attachmentList = select.execute();
		if (attachmentList.isEmpty()){
			return  null;
		}else {
			return attachmentList.get(0);
		}
	}

	@HIDDEN
	public InputStream getAttachment();
	public void setAttachment(InputStream attachment);

	@IS_VIRTUAL
	public String getAttachmentUrl();
	public void setAttachmentUrl(String url);


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
