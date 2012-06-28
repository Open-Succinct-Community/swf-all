package com.venky.swf.plugins.oauth.db.model;

import com.venky.swf.db.model.User;
import com.venky.swf.db.table.ModelImpl;

public class UserEmailImpl extends ModelImpl<UserEmail> {
	public UserEmailImpl(UserEmail proxy) {
		super(proxy);
	}

	public User getSelfUser() {
		return getProxy().getUser();
	}
}
