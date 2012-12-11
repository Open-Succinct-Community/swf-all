package com.venky.swf.plugins.bugs.extensions;

import java.io.IOException;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.plugins.bugs.db.model.Note;

public class IssueAfterSaveExtension extends AfterModelSaveExtension<Issue>{
	static {
		registerExtension(new IssueAfterSaveExtension());
	}
	@Override
	public void afterSave(Issue model) {
		Note note = Database.getTable(Note.class).newRecord();
		note.setIssueId(model.getId());
		boolean persist = false;
		if (!ObjectUtil.isVoid(model.getDescription()) && !ObjectUtil.isVoid(StringUtil.read(model.getDescription()))){
			note.setNotes(model.getDescription());
			persist = true; 
		}
		try {
			if (!ObjectUtil.isVoid(model.getAttachment()) && model.getAttachment().available() > 0 ){
				note.setAttachment(model.getAttachment());
				note.setAttachmentContentName(model.getAttachmentContentName());
				note.setAttachmentContentType(model.getAttachmentContentType());
				persist = true;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (persist){
			note.save();
		}
	}

}
