package com.venky.swf.controller;

import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;

public class UsersController extends ModelController<User>{

	public UsersController(Path path) {
		super(path);
	}

	@SingleRecordAction
	public View generateApiKey(int id){
		User u = Database.getTable(User.class).get(id);
		u.generateApiKey();
		StringBuilder message = new StringBuilder(); 
		message.append("API Key for ").append(u.getName()).append(" generated: (").append(u.getApiKey()).append(")");
		return new BytesView(getPath(), u.getApiKey().getBytes());
		
	}
}
