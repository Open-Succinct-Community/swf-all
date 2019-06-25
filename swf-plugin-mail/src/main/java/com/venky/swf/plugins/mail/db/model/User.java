package com.venky.swf.plugins.mail.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;


public interface User extends com.venky.swf.db.model.User{
	public void sendMail(String subject, String text, List<User> cc , List<User> bcc);
	public void sendMail(String subject, String text);

	@CONNECTED_VIA("USER_ID")
	public List<Mail> getSentMails();


}
