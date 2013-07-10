package com.venky.swf.plugins.mail.core;

import org.codemonkey.simplejavamail.Email;

public interface Mailer {
	public void sendMail(Email mail);
}
