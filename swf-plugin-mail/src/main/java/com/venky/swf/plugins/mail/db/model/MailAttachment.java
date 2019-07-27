package com.venky.swf.plugins.mail.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.attachment.db.model.Attachment;

public interface MailAttachment extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    public Long getMailId();
    public void setMailId(Long id);
    public Mail getMail();

    @UNIQUE_KEY
    public long getAttachmentId();
    public void setAttachmentId(long id);
    public Attachment getAttachment();

    @IS_VIRTUAL
    public String getName();
}
