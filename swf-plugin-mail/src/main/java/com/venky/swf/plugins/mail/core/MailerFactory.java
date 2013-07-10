package com.venky.swf.plugins.mail.core;

import java.util.HashMap;
import java.util.Map;

import com.venky.swf.plugins.mail.core.grid.SendGridMailer;
import com.venky.swf.plugins.mail.core.smtp.SMTPMailer;

public class MailerFactory {
	private static MailerFactory instance = null;
	public static MailerFactory instance(){ 
		if (instance != null){
			return instance;
		}
		synchronized (MailerFactory.class) {
			if (instance == null){
				instance = new MailerFactory();
			}
		}
		return instance;
	}
	private MailerFactory() {
		mailer.put("SMTP", new SMTPMailer());
		mailer.put("SENDGRID", new SendGridMailer());
	}
	private Map<String,Mailer> mailer = new HashMap<String, Mailer>();
	
	public Mailer getMailer(String protocol){
		Mailer m = mailer.get(protocol);
		if (m == null){
			throw new RuntimeException("Mailer not configured for protocol " + protocol);
		}
		return m;
	}
}
