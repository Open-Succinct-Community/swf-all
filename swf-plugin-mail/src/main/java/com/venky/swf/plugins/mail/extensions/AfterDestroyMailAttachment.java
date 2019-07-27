package com.venky.swf.plugins.mail.extensions;

import com.venky.swf.db.extensions.AfterModelDestroyExtension;
import com.venky.swf.plugins.attachment.db.model.Attachment;
import com.venky.swf.plugins.mail.db.model.MailAttachment;

public class AfterDestroyMailAttachment extends AfterModelDestroyExtension<MailAttachment> {
    static {
        registerExtension(new AfterDestroyMailAttachment());
    }
    @Override
    public void afterDestroy(MailAttachment model) {
        Attachment attachment = model.getAttachment();
        if (attachment != null){
            attachment.destroy();
        }
    }
}
