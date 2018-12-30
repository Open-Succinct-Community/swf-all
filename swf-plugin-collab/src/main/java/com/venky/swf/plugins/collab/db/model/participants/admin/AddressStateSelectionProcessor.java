package com.venky.swf.plugins.collab.db.model.participants.admin;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;
import com.venky.swf.db.model.Model;

public class AddressStateSelectionProcessor<M extends Address & Model> implements OnLookupSelectionProcessor<M>{

	public AddressStateSelectionProcessor() {
	}

	public void process(String fieldSelected, M partiallyFilledModel) {
		if (fieldSelected.equals("STATE_ID") && !Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getStateId())){
			partiallyFilledModel.setCountryId(partiallyFilledModel.getState().getCountryId());
		}
	}

}
