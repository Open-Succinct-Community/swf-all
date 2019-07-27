package com.venky.swf.plugins.mail.db.model;

import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.attachment.db.model.Attachment;

public class MailAttachmentImpl extends ModelImpl<MailAttachment> {
    public MailAttachmentImpl(){

    }
    public MailAttachmentImpl(MailAttachment attachment){
        super(attachment);
    }

    public String getName(){
        MailAttachment mailAttachment = getProxy();
        if (!mailAttachment.getReflector().isVoid(mailAttachment.getAttachmentId())){
            Attachment attachment =  mailAttachment.getAttachment();
            return attachment.getAttachmentContentName();
        }
        return null;
    }
}

