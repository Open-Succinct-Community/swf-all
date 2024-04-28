package com.venky.swf.plugins.collab.db.model.uom;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.OnLookupSelectionProcessor;

public class UnitOfMeasureSelectionProcessor implements OnLookupSelectionProcessor<UnitOfMeasureConversionTable>  {

	@Override
	public void process(String fieldSelected, UnitOfMeasureConversionTable partiallyFilledModel) {
		if (fieldSelected.equals("FROM_ID") &&
				!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getFromId())){
			if (!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getToId())){ 
				if (!ObjectUtil.equals(partiallyFilledModel.getFrom().getMeasures(),partiallyFilledModel.getTo().getMeasures())){
					partiallyFilledModel.setToId(null);
				}
			}
		}else if (fieldSelected.equals("TO_ID") && 
				!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getToId())){

			if (!Database.getJdbcTypeHelper(partiallyFilledModel.getReflector().getPool()).isVoid(partiallyFilledModel.getFromId())){
				if (ObjectUtil.equals(partiallyFilledModel.getFrom().getMeasures(),partiallyFilledModel.getTo().getMeasures())) {
					partiallyFilledModel.setFromId(null);
				}
			}
		}
		
	}

}
