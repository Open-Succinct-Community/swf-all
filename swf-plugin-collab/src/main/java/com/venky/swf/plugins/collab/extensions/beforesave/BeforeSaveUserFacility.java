package com.venky.swf.plugins.collab.extensions.beforesave;

import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;

public class BeforeSaveUserFacility extends BeforeModelSaveExtension<UserFacility>{
	static {
		registerExtension(new BeforeSaveUserFacility());
	}
	@Override
	public void beforeSave(UserFacility uf) {
		if (!uf.getFacility().isAccessibleBy(uf.getUser(), Facility.class)){
			throw new AccessDeniedException(uf.getUser().getName() + " cannot access " + uf.getFacility().getName());
		}
	}

}
