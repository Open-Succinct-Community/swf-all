package com.venky.swf.plugins.mail.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.mail.core.MailerTask;

import java.io.StringReader;

public class UserImpl extends ModelImpl<User>{

	public UserImpl(User proxy) {
		super(proxy);
	}
	public void sendMail(String subject, String text){
		MailerTask task = new MailerTask(getProxy(), subject, text);
		TaskManager.instance().executeAsync(task);
	}

}
