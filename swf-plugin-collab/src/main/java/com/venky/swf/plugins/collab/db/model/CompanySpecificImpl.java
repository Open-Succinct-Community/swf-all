package com.venky.swf.plugins.collab.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.ModelImpl;


public class CompanySpecificImpl<M extends Model>  extends ModelImpl<M>{
	
	public CompanySpecificImpl(M proxy) {
		super(proxy);
	}

	public Integer getCompanyCreatorUserId(){
		CompanySpecific cs = null ;
		if (CompanySpecific.class.isInstance(getProxy())){
			cs = (CompanySpecific)getProxy();
		}
				
		if (cs != null && !Database.getJdbcTypeHelper().isVoid(cs.getCompanyId())) {
			return cs.getCompany().getCreatorUserId();
		}
		return null; //root
	}
	
	
}
