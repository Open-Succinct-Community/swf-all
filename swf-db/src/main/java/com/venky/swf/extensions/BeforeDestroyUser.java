package com.venky.swf.extensions;

import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import com.venky.swf.db.model.User;

public class BeforeDestroyUser extends BeforeModelDestroyExtension<User>{
	static {
		registerExtension(new BeforeDestroyUser());
	}
	@Override
	public void beforeDestroy(User model) {
		if (model.isAdmin()){
			throw new UnsupportedOperationException("Application administrator cannot be deleted");
		}
	}

}
