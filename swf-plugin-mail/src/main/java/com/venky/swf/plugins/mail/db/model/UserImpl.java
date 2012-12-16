package com.venky.swf.plugins.mail.db.model;

import java.util.List;

import javax.mail.Message.RecipientType;

import org.codemonkey.simplejavamail.Email;
import org.codemonkey.simplejavamail.Mailer;

import com.venky.swf.db.model.UserEmail;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

public class UserImpl extends ModelImpl<User>{

	public UserImpl(User proxy) {
		super(proxy);
	}
	
	public void sendMail(User to, String subject, String text){
		List<UserEmail> emails = to.getUserEmails();
		if (emails.isEmpty()){
			throw new RuntimeException("No email available for " + to.getName());
		}
		
		List<UserEmail> fromEmails = getProxy().getUserEmails();
		if( fromEmails.isEmpty()) {
			throw new RuntimeException("No email available for " + getProxy().getName());
		}
		User from = getProxy();
		UserEmail fromEmail = fromEmails.get(0);
		UserEmail toEmail = emails.get(0);
		
		Email email = new Email();
		email.setFromAddress(from.getName(), fromEmail.getEmail());
		email.setSubject(subject);
		email.addRecipient(to.getName(), toEmail.getEmail(), RecipientType.TO);
		email.setText(text);
		
		new Mailer("smtp.googlemail.com", 465, Config.instance().getProperty("swf.sendmail.user"), Config.instance().getProperty("swf.sendmail.password")).sendMail(email);
	}

}
