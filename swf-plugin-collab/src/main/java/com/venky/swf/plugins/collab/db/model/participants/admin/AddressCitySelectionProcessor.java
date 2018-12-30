package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;
import com.venky.swf.db.model.Model;

public class AddressCitySelectionProcessor<M extends Model & Address> implements OnLookupSelectionProcessor<M>{

	public AddressCitySelectionProcessor() {
	}

	public void process(String fieldSelected, M partiallyFilledModel) {
		if (fieldSelected.equals("CITY_ID")  && !Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getCityId())){
			partiallyFilledModel.setStateId(partiallyFilledModel.getCity().getStateId());
			partiallyFilledModel.setCountryId(partiallyFilledModel.getState().getCountryId());
		}
	}

}
