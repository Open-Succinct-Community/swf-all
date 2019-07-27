package com.venky.swf.plugins.mail.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.mail.core.MailerTask.AttachedElement;


public interface User extends com.venky.swf.db.model.User{
	public void sendMail(String subject, String text, List<User> cc , List<User> bcc, List<AttachedElement> attachedElements);
	public void sendMail(String subject, String text);

	@CONNECTED_VIA("USER_ID")
	@HIDDEN
	public List<Mail> getSentMails();


}
