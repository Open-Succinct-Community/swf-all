package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;

import java.util.List;

@HAS_DESCRIPTION_FIELD("DESCRIPTION")
public interface MasterFacilityCategoryValue extends Model{
	@UNIQUE_KEY
	@PROTECTION(Kind.NON_EDITABLE)
	@PARTICIPANT
	@IS_NULLABLE(false)
	public Long getMasterFacilityCategoryId();
	public void setMasterFacilityCategoryId(Long id);
	public MasterFacilityCategory getMasterFacilityCategory();
	
	@UNIQUE_KEY
	public String getAllowedValue();
	public void setAllowedValue(String value);

	@IS_VIRTUAL
	public String getDescription();


	@CONNECTED_VIA("MASTER_FACILITY_CATEGORY_VALUE_ID")
	List<FacilityCategory> getFacilityCategories();
	
	
}
