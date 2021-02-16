package com.venky.swf.plugins.attachment.controller;

import java.util.List;

import com.venky.core.date.DateUtils;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.attachment.db.model.Attachment;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.View;

public class AttachmentsController extends ModelController<Attachment>{

	public AttachmentsController(Path path) {
		super(path);
	}


	@RequireLogin(false)
	public View view(String contentFileName){
		Attachment attachment = null;
		if (contentFileName.matches("^[0-9]+\\.[^\\.]+$")){
			String[] parts = contentFileName.split("\\.");
			attachment = Database.getTable(Attachment.class).get(Long.valueOf(parts[0]));
		}
		if (attachment == null){
			List<Attachment> attachments = new Select().from(Attachment.class).where(new Expression(getReflector().getPool(),"ATTACHMENT_CONTENT_NAME",Operator.EQ,contentFileName)).execute();
			if (attachments.size() == 1){
				attachment = attachments.get(0);
			}
		}
		if (attachment != null) {
			return view(attachment,null);
		}

		throw new AccessDeniedException();
	}

	@Override
	@RequireLogin(false)
	public View view(long id) {
		return super.view(id);
	}

	@Override
	public View save() {
		if (getIntegrationAdaptor() == null){
			String id = (String)getPath().getFormFields().get("ID");
			if (!ObjectUtil.isVoid(id)){
				Attachment attachment = Database.getTable(getModelClass()).get(Long.valueOf(id));
				if (attachment != null){
					attachment.destroy();
					getPath().getFormFields().remove("ID");
				}
			}
		}
		return super.save();
	}
}
