package com.venky.swf.plugins.bugs.db.model;

import com.venky.swf.db.table.ModelImpl;

public class NoteImpl extends ModelImpl<Note> {
    public NoteImpl(){
        super();
    }
    public NoteImpl(Note note){
        super(note);
    }
    public String getAttachmentUrl(){
        if (getProxy().getRawRecord().isNewRecord() || getProxy().getAttachmentContentSize() == 0){
            return null;
        }
        return "/notes/view/" + getProxy().getId();
    }
    public void setAttachmentUrl(String url){

    }

}
