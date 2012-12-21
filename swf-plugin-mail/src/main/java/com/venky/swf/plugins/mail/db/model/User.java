package com.venky.swf.plugins.mail.db.model;


public interface User extends com.venky.swf.db.model.User{
	public void sendMail(String subject, String text);
}
