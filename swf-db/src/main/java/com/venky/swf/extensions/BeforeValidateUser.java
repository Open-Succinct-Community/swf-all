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
			if (!ObjectUtil.isVoid(model.getName())){
				String name = model.getName();
				int at_position =  name.indexOf('@');
				if (at_position >= 0){
					name = name.substring(0,at_position);  //Avoid Special Characters of email id
				}

				model.setLongName(name);
			}
		}
	}

}
