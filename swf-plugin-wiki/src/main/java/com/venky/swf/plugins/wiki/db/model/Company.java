package com.venky.swf.plugins.wiki.db.model;

import java.util.List;

import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.HIDDEN;


public interface Company extends  com.venky.swf.plugins.collab.db.model.participants.admin.Company{
	@CONNECTED_VIA("COMPANY_ID")
	@HIDDEN
	public List<Page> getPages();
}
