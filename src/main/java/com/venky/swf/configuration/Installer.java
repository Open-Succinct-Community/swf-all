package com.venky.swf.configuration;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Query;
import com.venky.swf.db.table.Table;

public class Installer {
	public void install(){
		installUsers();
	}
	protected void installUsers(){
		Table<User> USER = Database.getInstance().getTable(User.class);
		Query q = new Query(User.class);
		List<User> users = q.select().where(" ( username = ? or email_id = ? )", new BindVariable("root"),new BindVariable("root@localhost.localdomain")).execute();
		
		if (users.isEmpty()){
			User u = USER.newRecord();
			u.setEmailId("root@localhost.localdomain");
			u.setMobileNo("+911234567890");
			u.setUsername("root");
			u.setPassword("root");
			u.save();
		}
	}
}
