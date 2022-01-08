package com.venky.swf.plugins.attachment.extensions;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.attachment.db.model.Attachment;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.net.URL;

public class BeforeValidateAttachment  extends BeforeModelValidateExtension<Attachment> {
    static {
        registerExtension(new BeforeValidateAttachment());
    }
    @Override
    public void beforeValidate(Attachment model) {
        if (!ObjectUtil.isVoid(model.getUploadUrl())){
            String fileName = model.getUploadUrl().substring(model.getUploadUrl().lastIndexOf("/")+1);
            //Call<?> call = new Call<>();

            ByteArrayInputStream inputStream = null;
            try {
                inputStream = (ByteArrayInputStream) new ByteArrayInputStream(StringUtil.readBytes(new URL(model.getUploadUrl()).openConnection().getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            model.setAttachment(inputStream);
            model.setAttachmentContentSize(inputStream.available());



            if (fileName.contains(".")){
                model.setAttachmentContentType(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName));
                model.setAttachmentContentName(fileName);
            }else {
                model.setAttachmentContentName("blob");
                model.setAttachmentContentType(MimeType.APPLICATION_OCTET_STREAM.toString());
            }

            {
                Attachment model2 = Database.getTable(Attachment.class).getRefreshed(model);
                model.getRawRecord().load(model2.getRawRecord());
                model.getRawRecord().setNewRecord(model2.getRawRecord().isNewRecord());
            }
        }

    }
}
