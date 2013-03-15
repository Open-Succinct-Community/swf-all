package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.CompanySpecificImpl;

public class UserEmailImpl extends CompanySpecificImpl<UserEmail> {

	public UserEmailImpl(UserEmail proxy) {
		super(proxy);
	}
	
	public Integer getCompanyId(){
		UserEmail ur = getProxy();
		if (!Database.getJdbcTypeHelper().isVoid(ur.getUserId())){
			User u = (User)ur.getUser();
			if (u != null) {
				return u.getCompanyId();
			}
		}
		return null;
	}
	public void setCompanyId(Integer id){
		
	}
}
