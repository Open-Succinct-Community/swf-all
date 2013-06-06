package com.venky.swf.plugins.wiki.controller;

import java.util.List;

import com.venky.swf.controller.ModelController;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.wiki.db.model.Attachment;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.View;

public class AttachmentsController extends ModelController<Attachment>{

	public AttachmentsController(Path path) {
		super(path);
		// TODO Auto-generated constructor stub
	}
	
	public View view(String contentFileName){
		List<Attachment> attachements = new Select().from(Attachment.class).where(new Expression("ATTACHMENT_CONTENT_NAME",Operator.EQ,contentFileName)).execute();
		if (attachements.size() == 1){
			return super.view(attachements.get(0).getId());
		}
		throw new AccessDeniedException();
	}

}
