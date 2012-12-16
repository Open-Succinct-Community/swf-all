package com.venky.swf.plugins.mail.db.model;

import java.io.Reader;

public interface User extends com.venky.swf.db.model.User{
	public void sendMail(User to, Reader text);
}
