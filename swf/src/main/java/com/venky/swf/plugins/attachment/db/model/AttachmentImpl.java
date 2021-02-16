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
        Attachment attachment = getProxy();
        if (attachment.getRawRecord().isNewRecord()){
            return null;
        }else {
            StringBuilder url = new StringBuilder("/attachments/view/" + getProxy().getId());
            String name = attachment.getAttachmentContentName();
            int dotIndex = name.lastIndexOf('.');
            if ( dotIndex > 0) {
                url.append(".").append(name.substring(dotIndex+1));
            }
            return url.toString();
        }
    }




    public void setAttachmentUrl(String url){
        
    }
}
