package com.venky.swf.plugins.collab.extensions.participation;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.uom.UnitOfMeasure;
import com.venky.swf.plugins.collab.db.model.uom.UnitOfMeasureConversionTable;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;


import java.util.List;


public class UnitOfMeasureParticipantExtension extends ParticipantExtension<UnitOfMeasureConversionTable>{
	static  {
		registerExtension(new UnitOfMeasureParticipantExtension());
	}
	@Override
	protected List<Long> getAllowedFieldValues(User user, UnitOfMeasureConversionTable partiallyFilledModel, String fieldName) {
		if (fieldName.equals("FROM_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getToId())){
				ModelReflector<UnitOfMeasure> ref = ModelReflector.instance(UnitOfMeasure.class);
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(UnitOfMeasure.class, user,
						new Expression(ref.getPool(), "MEASURES",Operator.EQ, partiallyFilledModel.getTo().getMeasures())));
			}else{
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(UnitOfMeasure.class, user));
			}
		}else if (fieldName.equals("TO_ID")){
			if (!Database.getJdbcTypeHelper(getReflector().getPool()).isVoid(partiallyFilledModel.getFromId())){
				ModelReflector<UnitOfMeasure> ref = ModelReflector.instance(UnitOfMeasure.class);
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(UnitOfMeasure.class, user,
						new Expression(ref.getPool(), "MEASURES",Operator.EQ, partiallyFilledModel.getFrom().getMeasures())));
			}else{
				return DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(UnitOfMeasure.class, user));
			}
		}
		return null;
	}

}
