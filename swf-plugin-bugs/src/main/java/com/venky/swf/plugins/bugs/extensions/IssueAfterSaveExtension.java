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
		boolean persist = false;
		String description = null;
		if (!ObjectUtil.isVoid(model.getDescription())){
			description = StringUtil.read(model.getDescription());
			if (!ObjectUtil.isVoid(description)){
				note.setNotes(model.getDescription());
				persist = true; 
			}
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
			User admin = Database.getTable(User.class).get(1);
			if (admin != null && !admin.getUserEmails().isEmpty()){
				admin.sendMail("Issue: " + model.getId() + " " + model.getTitle() + " (" + model .getResolution() + ")" , description == null ? "" : description);
			}
			User creator = (User)model.getCreatorUser();
			if (creator != null && !creator.getUserEmails().isEmpty()) {
				creator.sendMail("Issue: " + model.getId() + " " + model.getTitle() + " (" + model .getResolution() + ")" , description == null ? "" : description);
			}
		}
	}

}
