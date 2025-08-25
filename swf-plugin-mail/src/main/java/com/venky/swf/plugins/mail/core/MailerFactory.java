package com.venky.swf.plugins.mail.core;

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

	private MailerFactory() {
		mailer.put(PROTOCOL_SMTP, new SMTPMailer());
	}
	private final Map<String,Mailer> mailer = new HashMap<>();
	public void registerMailer(String protocol, Mailer m){
		mailer.put(protocol,m);
	}
	
	public Mailer getMailer(String protocol){
		return mailer.get(protocol);
	}
}
