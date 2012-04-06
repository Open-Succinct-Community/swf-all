package com.venky.swf.plugins.oauth.extensions;

import java.util.List;

import com.venky.extension.Registry;
import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.oauth.db.model.UserOid;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class UserBeforeDestroy extends BeforeModelDestroyExtension<User>{

	static {
		Registry.instance().registerExtension("User.before.destroy", new UserBeforeDestroy());
	}
	@Override
	public void beforeDestroy(User user) {
		ModelReflector<UserOid> ref = ModelReflector.instance(UserOid.class);
		
		Select select = new Select().from(UserOid.class).where(new Expression(ref.getColumnDescriptor("USER_ID").getName(),Operator.EQ,user.getId()));
		List<UserOid> oids = select.execute();
		
		
		while(!oids.isEmpty()){
			UserOid oid = oids.remove(oids.size()-1);
			oid.destroy();
		}
	}

	
}
