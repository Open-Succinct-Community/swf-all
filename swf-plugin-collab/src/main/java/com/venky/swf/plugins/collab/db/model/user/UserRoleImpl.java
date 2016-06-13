package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.CompanySpecificImpl;

public class UserRoleImpl extends CompanySpecificImpl<UserRole> {

	public UserRoleImpl(UserRole proxy) {
		super(proxy);
	}
	
	public Integer getCompanyId(){
		UserRole ur = getProxy();
		if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(ur.getUserId())){
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
