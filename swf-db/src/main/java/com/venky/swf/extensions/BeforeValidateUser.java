package com.venky.swf.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.User;

public class BeforeValidateUser extends BeforeModelValidateExtension<User>{
	static {
		registerExtension(new BeforeValidateUser());
	}
	
	@Override
	public void beforeValidate(User model) {
		if (ObjectUtil.isVoid(model.getLongName())){
			model.setLongName(model.getName());
		}
	}

}
