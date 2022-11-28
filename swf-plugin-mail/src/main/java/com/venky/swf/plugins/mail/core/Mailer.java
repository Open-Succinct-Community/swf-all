package com.venky.swf.plugins.mail.core;

import org.codemonkey.simplejavamail.email.Email;

public interface Mailer {
	public void sendMail(Email mail);
}
