package com.venky.swf.plugins.oauth.extensions;

import java.util.List;

import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.oauth.db.model.UserEmail;
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
		Expression expression = new Expression(Conjunction.AND);
		expression.add(new Expression("email",Operator.IN, model.getEmail()));
		expression.add(new Expression("user_id",Operator.NE,model.getUserId()));
		Select select = new Select().from(UserEmail.class).where(expression);
		List<UserEmail> r = select.execute(UserEmail.class);
		if (!r.isEmpty()){
			throw new AccessDeniedException("Email belongs to different user!");
		}
	}

}
