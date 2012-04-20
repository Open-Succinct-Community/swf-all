package com.venky.swf.db.table;

import com.venky.swf.db.model.Model;

public class ModelImpl<M extends Model> extends ModelInvocationHandler{
	public ModelImpl(M proxy){
		super((Class<? extends Model>)proxy.getClass().getInterfaces()[0],proxy.getRawRecord());
		setProxy(proxy);
	}
	
	public M getProxy(){
		return super.getProxy();
	}
	
}
