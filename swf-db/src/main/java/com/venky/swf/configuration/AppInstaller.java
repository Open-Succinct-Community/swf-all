package com.venky.swf.configuration;

import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.BindVariable;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.sql.Update;
import com.venky.swf.sql.parser.SQLExpressionParser.EQ;

public class AppInstaller implements Installer {
	public void install(){
		installUsers();
		fixUserName();
		fixUserPasswords();
	}
	protected void fixUserPasswords(){
		if (Config.instance().shouldPasswordsBeEncrypted()){
			List<User> users = new Select().from(User.class).
					where(new Expression(ModelReflector.instance(User.class).getPool(),
							"PASSWORD_ENCRYPTED", Operator.EQ,false)).execute();
			for (User user : users) {
				String encryptedPassword = user.getEncryptedPassword(user.getPassword());
				user.setPassword(encryptedPassword);
				user.setPasswordEncrypted(true);
				user.save();
			}
		}
	}
	protected void fixUserName(){
		Select q = new Select().from(User.class);
		ModelReflector<User> ref = ModelReflector.instance(User.class);
		String nameColumn = ref.getColumnDescriptor("long_name").getName();

		List<User> users = q.where(new Expression(ref.getPool(),nameColumn,Operator.EQ)).execute();
		for (User user: users){
			user.setLongName(user.getName());
		}
	}
	
	protected void installUsers(){
		Table<User> USER = Database.getTable(User.class);
		
		Select q = new Select().from(User.class);
		ModelReflector<User> ref = ModelReflector.instance(User.class);
		String nameColumn = ref.getColumnDescriptor("name").getName();
		
		
		List<User> users = q.where(new Expression(ref.getPool(),nameColumn,Operator.EQ,new BindVariable(ref.getPool(),"root"))).execute(User.class);
		
		if (users.isEmpty()){
			User u = USER.newRecord();
			u.setName("root");
			u.setLongName("Application Adminstrator");
			u.setPassword("root");
			u.setPasswordEncrypted(false);
			u.save();
			if (u.getId() != 1){
				new Update(ref).set("ID", new BindVariable(ref.getPool(),1L)).where(new Expression(ref.getPool(),"ID",Operator.EQ,u.getId())).executeUpdate();
				u.setId(1L); //Coorect memory object.
			}
		}
	}
}
