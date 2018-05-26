package com.venky.swf.plugins.mail.core;

import java.util.List;

import javax.mail.Message.RecipientType;

import org.codemonkey.simplejavamail.Email;

import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.mail.db.model.SentMail;
import com.venky.swf.plugins.mail.db.model.User;
import com.venky.swf.routing.Config;

public class MailerTask implements Task{

	private static final long serialVersionUID = 8083486775891668308L;
	
	long toUserId ;
	String subject; 
	String text; 
	boolean isHtml = false;
	public MailerTask(User to,String subject, String text){
		this.toUserId = to.getId();
		this.subject = subject;
		this.text = text;
		if (!ObjectUtil.isVoid(text)){
			int len = text.trim().length();
			if (len > 5 ) { 
				this.isHtml = text.trim().substring(0, 5).equalsIgnoreCase("<html");
			}
		}
	}
	
	public void execute() {
		User to = Database.getTable(User.class).get(toUserId);
		if (to == null){
			return;
		}
		List<UserEmail> emails = to.getUserEmails();
		if (emails.isEmpty()){
			throw new RuntimeException("No email available for " + to.getName());
		}
		
		String emailId = Config.instance().getProperty("swf.sendmail.user");
		String userName = Config.instance().getProperty("swf.sendmail.user.name");
		
		if( ObjectUtil.isVoid(emailId)) {
			throw new RuntimeException("Plugin not configured :swf.sendmail.user" );
		}
		
		final Email email = new Email();
		email.setFromAddress(userName, emailId);
		email.setSubject(subject);
		StringBuilder emailString = new StringBuilder();
		for (UserEmail toEmail:emails){
			email.addRecipient(to.getName() + "(" + toEmail.getEmail() + ")", toEmail.getEmail(), RecipientType.TO);
			emailString.append(toEmail.getEmail()).append(";");
		}
		if (isHtml){
			email.setTextHTML(text);
		}else {
			email.setText(text);
		}
		
		SentMail mail = Database.getTable(SentMail.class).newRecord();
		mail.setUserId(toUserId);
		mail.setEmail(emailString.toString());
		mail.setSubject(subject);
		mail.setBody(new StringReader(text));
		mail.save();

		AsyncMailer.instance().addEmail(email);
	}

}
