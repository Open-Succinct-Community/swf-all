package com.venky.swf.plugins.mail.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.mail.core.MailerTask;
import com.venky.swf.plugins.mail.core.MailerTask.AttachedElement;

import java.io.StringReader;
import java.util.List;

public class UserImpl extends ModelImpl<User>{

	public UserImpl(User proxy) {
		super(proxy);
	}
	public void sendMail(String subject, String text){
		sendMail(subject,text,null,null,null);
	}
	public void sendMail(String subject, String text, List<User> cc , List<User> bcc, List<AttachedElement> attachedElements){
		MailerTask task = new MailerTask(getProxy(), subject, text, cc,bcc,attachedElements);
		TaskManager.instance().executeAsync(task);
	}

}
