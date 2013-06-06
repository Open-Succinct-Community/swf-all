package com.venky.swf.plugins.mail.db.model;

import java.util.List;


public interface User extends com.venky.swf.db.model.User{
	public void sendMail(String subject, String text);
	public List<SentMail> getSentMails();
}
