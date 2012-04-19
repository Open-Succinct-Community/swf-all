package com.venky.swf.db.table;

import com.venky.swf.db.model.Model;

public class ModelImpl<M extends Model> extends ModelInvocationHandler<M>{
	public ModelImpl(M proxy){
		super((Class<M>)proxy.getClass().getInterfaces()[0],proxy.getRawRecord());
		setProxy(proxy);
	}
	
}
