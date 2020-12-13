package com.venky.swf.plugins.bugs.extensions;

import java.io.IOException;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.bugs.db.model.Issue;
import com.venky.swf.plugins.bugs.db.model.Note;
import com.venky.swf.plugins.mail.db.model.User;

public class IssueAfterSaveExtension extends AfterModelSaveExtension<Issue>{
	static {
		registerExtension(new IssueAfterSaveExtension());
	}
	@Override
	public void afterSave(Issue model) {
		Note note = Database.getTable(Note.class).newRecord();
		note.setIssueId(model.getId());
		boolean persistNote = false;
		String description = null;
		if (!ObjectUtil.isVoid(model.getDescription())){
			description = StringUtil.read(model.getDescription());
			if (!ObjectUtil.isVoid(description)){
				note.setNotes(model.getDescription());
				persistNote = true; 
			}
		}
		try {
			if (!ObjectUtil.isVoid(model.getAttachment()) && model.getAttachment().available() > 0 ){
				note.setAttachment(model.getAttachment());
				note.setAttachmentContentName(model.getAttachmentContentName());
				note.setAttachmentContentType(model.getAttachmentContentType());
				note.setAttachmentContentSize(model.getAttachmentContentSize());
				persistNote = true;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (persistNote){
			note.save();
			User admin = Database.getTable(User.class).get(1);
			if (admin != null && !admin.getUserEmails().isEmpty()){
				String resolution = model.getResolution();
				admin.sendMail("Issue: " + model.getId() + " " + model.getTitle() +  (resolution == null ? "" : " (" + resolution + ")") , description == null ? "" : description);
			}
			User creator = (User)model.getCreatorUser();
			if (creator != null && !creator.getUserEmails().isEmpty()) {
				if (admin == null || admin.getId() != creator.getId()) {
		 			String resolution = model.getResolution();
					creator.sendMail("Issue: " + model.getId() + " " + model.getTitle() + (resolution == null ? "" : " (" + resolution + ")" ) , description == null ? "" : description);
				}
			}
		}
	}

}
