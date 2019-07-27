package com.venky.swf.plugins.mail.extensions;

import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.attachment.db.model.Attachment;
import com.venky.swf.plugins.mail.db.model.MailAttachment;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import java.util.List;

public class BeforeDestroyAttachment extends BeforeModelDestroyExtension<Attachment> {
    @Override
    public void beforeDestroy(Attachment model) {
        ModelReflector<MailAttachment> ref = ModelReflector.instance(MailAttachment.class);

        List<MailAttachment> mailAttachments = new Select().from(MailAttachment.class).where(new Expression(ref.getPool(),"ATTACHMENT_ID", Operator.EQ , model.getId())).execute();
        if (!mailAttachments.isEmpty()){
            throw new RuntimeException("There are mails refering to this attachment!. Please delete the mail. " + mailAttachments.get(0).getMailId());
        }
    }
}
