package com.venky.swf.plugins.mail.core;

import com.venky.swf.plugins.mail.core.grid.SendGridMailer;
import com.venky.swf.plugins.mail.core.smtp.SMTPMailer;

import java.util.HashMap;
import java.util.Map;

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
	public static final String PROTOCOL_SMTP = "SMTP";
	public static final String PROTOCOL_SEND_GRID = "SENDGRID";

	private MailerFactory() {
		mailer.put(PROTOCOL_SMTP, new SMTPMailer());
		mailer.put(PROTOCOL_SEND_GRID, new SendGridMailer());
	}
	private Map<String,Mailer> mailer = new HashMap<String, Mailer>();
	public void registerMailer(String protocol, Mailer m){
		mailer.put(protocol,m);
	}
	
	public Mailer getMailer(String protocol){
		Mailer m = mailer.get(protocol);
		return m;
	}
}
