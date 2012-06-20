package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.table.ModelImpl;

public class UserImpl extends ModelImpl<User>{

	public UserImpl(User proxy) {
		super(proxy);
	}
	
	public User getSelfUser(){
		return getProxy();
	}
}
