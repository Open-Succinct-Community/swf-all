package com.venky.swf.controller;

import java.util.Arrays;
import java.util.List;

import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import com.venky.swf.views.model.ModelListView;
import com.venky.swf.views.model.ModelShowView;

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
		if (getIntegrationAdaptor() != null){
			return getIntegrationAdaptor().createResponse(getPath(), u,Arrays.asList("API_KEY"));
		}else {
			return new BytesView(getPath(), u.getApiKey().getBytes());
		}
		
	}
	
    protected View constructModelListView(List<User> records){
    	@SuppressWarnings("unchecked")
		ModelListView<User> v = (ModelListView<User>) super.constructModelListView(records);
    	v.getIncludedFields().remove("CHANGE_PASSWORD");
    	return v;
    }
    protected ModelShowView<User> constructModelShowView(Path path, User record){
    	ModelShowView<User> v = new ModelShowView<User>(path, getIncludedFields(), record);
    	v.getIncludedFields().remove("CHANGE_PASSWORD");
    	return v;
    }

}
