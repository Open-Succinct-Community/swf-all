package com.venky.swf.configuration;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class Installer {
	public void install(){
		installUsers();
	}
	protected void installUsers(){
		Table<User> USER = Database.getInstance().getTable(User.class);
		
		Select q = new Select().from(Database.getInstance().getTable(User.class).getTableName());
		ModelReflector<User> ref = ModelReflector.instance(User.class);
		String nameColumn = ref.getColumnDescriptor("name").getName();
		
		
		List<User> users = q.where(new Expression(nameColumn,Operator.EQ,new BindVariable("root"))).execute();
		
		if (users.isEmpty()){
			User u = USER.newRecord();
			u.setName("root");
			u.setPassword("root");
			u.save();
		}
	}
}
