package com.venky.swf.plugins.mail.core;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.routing.Config;
import org.codemonkey.simplejavamail.email.Email;

import javax.mail.Message.RecipientType;

public class SimpleMailerTask implements Task{

	private static final long serialVersionUID = 8083486775891668308L;


	@Deprecated
	public SimpleMailerTask(){

	}
	
	private String toEmail;
	private String subject;
	private String text;
	private boolean isHtml = false;
	public SimpleMailerTask(String toEmail, String subject, String text){
		this.toEmail = toEmail;
		this.subject = subject;
		this.text = text;
		if (!ObjectUtil.isVoid(text)){
			String trim = text.trim();
			int len = trim.length();
			if (len > 5 ) { 
				this.isHtml = trim.substring(0, 5).equalsIgnoreCase("<html") || (len > 14 && trim.substring(0,14).equalsIgnoreCase("<!DOCTYPE html"));
			}
		}
	}
	
	public void execute() {
		
		String emailId = Config.instance().getProperty("swf.sendmail.user");
		String userName = Config.instance().getProperty("swf.sendmail.user.name");
		
		if( ObjectUtil.isVoid(emailId)) {
			throw new RuntimeException("Plugin not configured :swf.sendmail.user" );
		}
		
		final Email email = new Email();
		email.setFromAddress(userName, emailId);
		email.setSubject(subject);
		email.addRecipient(toEmail,toEmail,RecipientType.TO);
		if (isHtml){
			email.setTextHTML(text);
		}else {
			email.setText(text);
		}

		AsyncMailer.instance().addEmail(email);
	}

}
