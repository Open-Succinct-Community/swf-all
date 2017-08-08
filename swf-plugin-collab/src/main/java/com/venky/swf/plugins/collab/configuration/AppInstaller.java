package com.venky.swf.plugins.collab.configuration;

import java.util.List;

import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.collab.db.model.user.UserCompany;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
public class AppInstaller implements Installer{

  public void install() {
	  List<User> users = new Select().from(User.class).where(new Expression(ModelReflector.instance(User.class).getPool(), "COMPANY_ID", Operator.NE)).execute(User.class);
	  users.forEach(u->{
		  UserCompany uc = Database.getTable(UserCompany.class).newRecord();
		  uc.setUserId(u.getId());
		  uc.setCompanyId(u.getCompanyId());
		  uc.save();
		  u.setCompanyId(null);
	  });
  }
}

