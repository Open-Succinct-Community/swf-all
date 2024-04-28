package com.venky.swf.plugins.collab.db.model.uom;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.OnLookupSelect;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;

@CONFIGURATION
public interface UnitOfMeasureConversionTable extends Model{
	@IS_NULLABLE(false)
	@UNIQUE_KEY
	@OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.uom.UnitOfMeasureSelectionProcessor")
	@PARTICIPANT(redundant=true)
	public Long getFromId();
	public void setFromId(Long fromId);
	public UnitOfMeasure getFrom();
	
	@IS_NULLABLE(false)
	@UNIQUE_KEY
	@OnLookupSelect(processor="com.venky.swf.plugins.collab.db.model.uom.UnitOfMeasureSelectionProcessor")
	@PARTICIPANT(redundant=true)
	
	public Long getToId(); 
	public void setToId(Long toId); 
	public UnitOfMeasure getTo();
	
	public double getConversionFactor(); 
	public void setConversionFactor(double factor);
	
	public static double convert(double measurement, String measureType, String fromUom, String toUom ) {
		if (fromUom.equals(toUom)) {
			return measurement;
		}

		UnitOfMeasure from = UnitOfMeasure.getMeasure(measureType, fromUom);
		UnitOfMeasure to = UnitOfMeasure.getMeasure(measureType, toUom);
		if (from == null) {
			throw new IllegalArgumentException(fromUom + " is not a valid uom for " + measureType);
		}else if (to == null) {
			throw new IllegalArgumentException(toUom + " is not a valid uom for " + measureType);
		}

		return convert(measurement,measureType,from,to);
	}

	public static double convert(double measurement, String measureType, UnitOfMeasure from, UnitOfMeasure to ) {
		if (from.getId() != to.getId()) {
			for (UnitOfMeasureConversionTable cnvTable: from.getConvertableToUOMs()) {
				if (cnvTable.getToId() == to.getId()) {
					return cnvTable.getConversionFactor() * measurement;
				}
			}
			for (UnitOfMeasureConversionTable cnvTable: to.getConvertableToUOMs()) {
				if (cnvTable.getToId() == from.getId()) {
					return  measurement / cnvTable.getConversionFactor() ;
				}
			}
		}else {
			return measurement;
		}
		throw new IllegalArgumentException("Cannot be converted from " + from.getName() + " to " + to.getName());
	}
}
