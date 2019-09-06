package com.venky.swf.extensions;

import java.util.List;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.UserEmail;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class BeforeUserEmailSave extends BeforeModelSaveExtension<UserEmail> {
	static {
		registerExtension(new BeforeUserEmailSave());
	}
	@Override
	public void beforeSave(UserEmail model) {
		if (ObjectUtil.isVoid(model.getAlias())){
			User user = model.getUser();
			model.setAlias(user.getReflector().isVoid(user.getLongName()) ? user.getName() : user.getLongName());
		}
	}

}
