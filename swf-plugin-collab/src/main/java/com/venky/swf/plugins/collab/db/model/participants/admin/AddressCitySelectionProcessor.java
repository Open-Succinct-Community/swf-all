package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;

public class AddressCitySelectionProcessor implements OnLookupSelectionProcessor<Address>{

	public AddressCitySelectionProcessor() {
	}

	public void process(String fieldSelected, Address partiallyFilledModel) {
		if (fieldSelected.equals("CITY_ID")  && !Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getCityId())){
			partiallyFilledModel.setStateId(partiallyFilledModel.getCity().getStateId());
			partiallyFilledModel.setCountryId(partiallyFilledModel.getState().getCountryId());
		}
	}

}
