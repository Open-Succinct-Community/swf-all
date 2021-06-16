package com.venky.swf.plugins.attachment.extensions;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.plugins.attachment.db.model.Attachment;

import java.util.List;

public class BeforeValidateAttachment  extends BeforeModelValidateExtension<Attachment> {
    static {
        registerExtension(new BeforeValidateAttachment());
    }
    @Override
    public void beforeValidate(Attachment model) {
        if (!ObjectUtil.isVoid(model.getUploadUrl())){
            String fileName = model.getUploadUrl().substring(model.getUploadUrl().lastIndexOf("/")+1);
            Call<?> call = new Call<>();
            ByteArrayInputStream inputStream = (ByteArrayInputStream) call.url(model.getUploadUrl()).method(HttpMethod.GET).getResponseStream();

            model.setAttachment(inputStream);
            model.setAttachmentContentSize(inputStream.available());

            List<String> contentTypes = call.getResponseHeaders().get("content-type");
            model.setAttachmentContentType(contentTypes.get(0));
            model.setAttachmentContentName(fileName == null ? "blob" : fileName);

            {
                Attachment model2 = Database.getTable(Attachment.class).getRefreshed(model);
                model.getRawRecord().load(model2.getRawRecord());
                model.getRawRecord().setNewRecord(false);
            }
        }

    }
}
