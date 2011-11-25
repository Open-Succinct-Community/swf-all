package com.venky.swf.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Record;
import com.venky.swf.exceptions.AccessDeniedException;

public class UserImpl<M extends User> extends ModelImpl<M>{
	
	public UserImpl(Class<M> modelClass, Record record) {
		super(modelClass, record);
	}

	
	public boolean authenticate(String password){
		try {
			User user = getProxy();
			if (Registry.instance().hasExtensions(User.USER_AUTHENTICATE)){
				Registry.instance().callExtensions(User.USER_AUTHENTICATE, user,password);
			}else {
				ObjectUtil.equals(user.getPassword(),password);
			}
			return true;
		}catch (AccessDeniedException ex){
			return false;
		}

	}
}
