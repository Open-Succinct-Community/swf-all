package com.venky.swf.plugins.mail.core.smtp;

import com.venky.swf.routing.Config;
import org.codemonkey.simplejavamail.Mailer;
import org.codemonkey.simplejavamail.TransportStrategy;
import org.codemonkey.simplejavamail.email.Email;

public class SMTPMailer implements com.venky.swf.plugins.mail.core.Mailer {

	private Mailer mailer = null;
	public SMTPMailer() {
		String account = Config.instance().getProperty("swf.sendmail.account");
		String password = Config.instance().getProperty("swf.sendmail.password");
		String host = Config.instance().getProperty("swf.sendmail.smtp.host","smtp.gmail.com");
		int port = Config.instance().getIntProperty("swf.sendmail.smtp.port",465);
		String auth = Config.instance().getProperty("swf.sendmail.smtp.auth");
		TransportStrategy transportStrategy  =  auth == null ? TransportStrategy.SMTP_PLAIN : TransportStrategy.valueOf(auth);

		mailer = new Mailer(host, port, account,password,transportStrategy);
	}
	public void sendMail(Email email){
		mailer.sendMail(email);
	}
}
