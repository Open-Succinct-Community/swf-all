package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;

public class AddressStateSelectionProcessor implements OnLookupSelectionProcessor<Address>{

	public AddressStateSelectionProcessor() {
	}

	public void process(String fieldSelected, Address partiallyFilledModel) {
		if (fieldSelected.equals("STATE_ID") && !Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getStateId())){
			partiallyFilledModel.setCountryId(partiallyFilledModel.getState().getCountryId());
		}
	}

}
