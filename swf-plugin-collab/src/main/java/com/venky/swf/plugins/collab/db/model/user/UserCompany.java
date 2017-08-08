package com.venky.swf.plugins.collab.db.model.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;


public interface UserCompany extends Model {
	@PARTICIPANT
	@UNIQUE_KEY
	@HIDDEN
	public int getUserId();
	public void setUserId(int userId); 
	public User getUser(); 
	
	@IS_NULLABLE(false)
	@Index
	@UNIQUE_KEY
	public Integer getCompanyId();
	public void setCompanyId(Integer id);
	public Company getCompany();

}
