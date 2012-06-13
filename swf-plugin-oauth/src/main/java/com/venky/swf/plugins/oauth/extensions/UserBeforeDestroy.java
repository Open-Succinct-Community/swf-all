package com.venky.swf.plugins.oauth.extensions;

import java.util.List;

import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.oauth.db.model.UserEmail;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class UserBeforeDestroy extends BeforeModelDestroyExtension<User>{

	static {
		registerExtension(new UserBeforeDestroy());
	}
	@Override
	public void beforeDestroy(User user) {
		ModelReflector<UserEmail> ref = ModelReflector.instance(UserEmail.class);
		
		Select select = new Select().from(UserEmail.class).where(new Expression(ref.getColumnDescriptor("USER_ID").getName(),Operator.EQ,user.getId()));
		List<UserEmail> oids = select.execute(UserEmail.class);
		
		
		while(!oids.isEmpty()){
			UserEmail oid = oids.remove(oids.size()-1);
			oid.destroy();
		}
	}

	
}
