package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.table.ModelImpl;

public class CompanyImpl extends ModelImpl<Company>{

	public CompanyImpl(Company proxy) {
		super(proxy);
	}

	public Company getSelfCompany(){
		return getProxy();
	}
}
