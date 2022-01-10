package com.venky.swf.plugins.attachment.extensions;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.plugins.attachment.db.model.Attachment;
import org.owasp.encoder.Encode;

import javax.activation.MimetypesFileTypeMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

public class BeforeValidateAttachment  extends BeforeModelValidateExtension<Attachment> {
    static {
        registerExtension(new BeforeValidateAttachment());
    }
    @Override
    public void beforeValidate(Attachment model) {
        if (!ObjectUtil.isVoid(model.getUploadUrl())){
            try {
                URL url = new URL(Encode.forUriComponent(model.getUploadUrl()));
                if (!url.getProtocol().startsWith("data")){
                    String fileName = model.getUploadUrl().substring(model.getUploadUrl().lastIndexOf("/")+1);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(StringUtil.readBytes(url.openStream()));
                    model.setAttachment(inputStream);
                    model.setAttachmentContentSize(inputStream.available());
                    model.setAttachmentContentName(fileName);
                    if (fileName.contains(".")){
                        model.setAttachmentContentType(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(fileName));
                    }else {
                        model.setAttachmentContentType(MimeType.APPLICATION_OCTET_STREAM.toString());
                    }

                    {
                        Attachment model2 = Database.getTable(Attachment.class).getRefreshed(model);
                        model.getRawRecord().load(model2.getRawRecord());
                        model.getRawRecord().setNewRecord(model2.getRawRecord().isNewRecord());
                    }
                }
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }

        }

    }
}
