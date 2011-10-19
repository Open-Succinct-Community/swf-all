package com.venky.swf.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Record;

public class UserImpl<M extends User> extends ModelImpl<M>{

	public UserImpl(Class<M> modelClass, Record record) {
		super(modelClass, record);
	}
	
	public boolean authenticate(String password){
		return ObjectUtil.equals(getProxy().getPassword(),password);
	}
}
