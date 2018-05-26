package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.plugins.collab.db.model.user.UserFacility;

@IS_VIRTUAL
public interface FacilityUser extends UserFacility{
	@HIDDEN(false)
	public long getUserId();
	
	@HIDDEN(true)
	public long getFacilityId();
	
}
