package com.venky.swf.plugins.attachment.db.model;

import com.venky.swf.db.table.ModelImpl;

public class AttachmentImpl extends ModelImpl<Attachment> {
    public  AttachmentImpl(Attachment proxy){
        super(proxy);
    }
    public AttachmentImpl(){
        super();
    }
    public String getAttachmentUrl(){
        if (getProxy().getRawRecord().isNewRecord()){
            return null;
        }else {
            return "/attachments/view/" + getProxy().getId();
        }
    }

    public void setAttachmentUrl(String url){
        
    }
}
