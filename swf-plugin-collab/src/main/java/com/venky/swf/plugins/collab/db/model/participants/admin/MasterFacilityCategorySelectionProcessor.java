package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;

public class MasterFacilityCategorySelectionProcessor implements OnLookupSelectionProcessor<FacilityCategorization>  {

	@Override
	public void process(String fieldSelected, FacilityCategorization partiallyFilledModel) {
		if (fieldSelected.equals("MASTER_FACILITY_CATEGORY_VALUE_ID") &&
				!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getMasterFacilityCategoryValueId())){
			partiallyFilledModel.setMasterFacilityCategoryId(partiallyFilledModel.getMasterFacilityCategoryValue().getMasterFacilityCategoryId());
		}else if (fieldSelected.equals("MASTER_FACILITY_CATEGORY_ID") && 
				!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getMasterFacilityCategoryId())){

			if (!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getMasterFacilityCategoryValueId())){
				if (partiallyFilledModel.getMasterFacilityCategoryId() != partiallyFilledModel.getMasterFacilityCategoryValue().getMasterFacilityCategoryId()) {
					partiallyFilledModel.setMasterFacilityCategoryValueId(null);
				}
			}
		}
		
	}

}
