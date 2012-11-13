package com.venky.swf.db.model.reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class ModelReader<M extends Model> extends ModelIO<M> {

	public ModelReader(Class<M> modelClass) {
		super(modelClass);
	}
	
	public List<M> read(Sheet sheet){
		Iterator<Row> rowIterator = sheet.iterator();
        Row header = rowIterator.hasNext() ? rowIterator.next() : null;
        List<M> records = new ArrayList<M>();
        if (header == null){
        	return records;
        }
        
        String[] heading = new String[header.getLastCellNum()]; // as cell indexes start at zero,LastCellNum can be seen as Size of row  
        Map<String,Integer> headingIndexMap = new HashMap<String, Integer>();
        for (int i = 0 ; i < heading.length ; i ++ ){
            heading[i] = header.getCell(i).getStringCellValue();
            headingIndexMap.put(heading[i], i);
        }
        
        
        
        while (rowIterator.hasNext()){
        	Row row = rowIterator.next();
        	M m = createInstance();
    		copyRowValuesToBean(m, row, heading,headingIndexMap);
        	records.add(m);
        }
        return records;
	}

	public CellStyle getHeaderStyle(Sheet sheet){ 
        return sheet.getRow(0).getCell(0).getCellStyle();
    }
    
	protected Object getCellValue(Cell cell, Class<?> hint){
		Object value = null;
		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					value = cell.getDateCellValue();
				} else {
					value = cell.getNumericCellValue();
				}
				break;
			case Cell.CELL_TYPE_BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
			case Cell.CELL_TYPE_FORMULA:
				try {
					if (isDate(hint)) {
						value = cell.getDateCellValue();
					} else if (isBoolean(hint)) {
						value = cell.getBooleanCellValue();
					} else if (isNumeric(hint)) {
						value = cell.getNumericCellValue();
					} else {
						value = cell.getStringCellValue();
					}
				}catch (IllegalStateException ex){
					value = cell.getStringCellValue();
				}
				break;
			default:
				value = cell.getStringCellValue();
				break;
		}
		if (value != null && value instanceof String){
			value = ((String)value).trim();
		}
		return value;
	}
	protected void copyRowValuesToBean(M m, Row row,String[] heading,Map<String, Integer> headingIndexMap) {
		ModelReflector<M> ref = getReflector();
		SequenceSet<String> handledReferenceFields = new SequenceSet<String>();
		
		for (int i = 0; i < heading.length; i++) {
			Method getter = getGetter(heading[i]);
			if (getter == null) {
				continue;
			}

			GetterType type = GetterType.UNKNOWN_GETTER;
			String fieldName = null;

			if (ref.getFieldGetterMatcher().matches(getter)) {
				type = GetterType.FIELD_GETTER;
				fieldName = ref.getFieldName(getter);
			} else if (ref.getReferredModelGetterMatcher().matches(getter)) {
				type = GetterType.REFERENCE_MODEL_GETTER;
				fieldName = ref.getReferenceField(getter);
			}
			if (fieldName == null){
				continue;
			}
			Method setter = null;
			if (ref.isFieldSettable(fieldName)){
				setter = ref.getFieldSetter(fieldName);
			}
			if (setter == null) {
				continue;
			}

			Cell cell = row.getCell(i);
			Object value = null;
			if (cell == null) {
				continue;
			}
			if (type == GetterType.REFERENCE_MODEL_GETTER ) {
				if (handledReferenceFields.contains(fieldName)){
					continue;
				}
				handledReferenceFields.add(fieldName);
				String baseFieldHeading = getter.getName().substring("get".length());

				Class<? extends Model> referredModelClass = ref.getReferredModelClass(getter);
				ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
				
				SequenceSet<String> refModelFields = new SequenceSet<String>();
				loadFieldsToExport(refModelFields, baseFieldHeading, referredModelReflector);
				
				Map<String,Cell> fieldValues = new HashMap<String, Cell>();
				boolean referenceFieldsPassed = false;
				for (String field:refModelFields){
					Cell cell1 = row.getCell(headingIndexMap.get(field));
					if (cell1.getCellType() != Cell.CELL_TYPE_BLANK) {
						referenceFieldsPassed = true;
					}
					fieldValues.put(field.substring(field.indexOf('.')+1), cell1);
				}

				Model referredModel = getModel(referredModelReflector,fieldValues);
				if (referredModel == null && referenceFieldsPassed){
					throw new RuntimeException(referredModelClass.getSimpleName() + " not found for passed information " + fieldValues.toString());
				}else if (referredModel != null){
					value = referredModel.getId();
				}else {
					value = null;
				}
			} else if (type == GetterType.FIELD_GETTER) {
				TypeRef<?> tref = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType());
				TypeConverter<?> converter = tref.getTypeConverter();
				value = getCellValue(cell,getter.getReturnType());
				if (!ObjectUtil.isVoid(value) || ref.isFieldMandatory(fieldName)) {
					value = converter.valueOf(value);
				} else {
					value = null;
				}
			}

			try {
				setter.invoke(m, value);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot set " + heading[i] + " as "
						+ value + " of Class " + value.getClass().getName(), e);
			}
		}

	}

	private Model getModel(ModelReflector<? extends Model> reflector, Map<String, Cell> headingValues) {
		Expression where = new Expression(Conjunction.AND);

		Cache<String,Map<String,Cell>> newHeadingValues = new Cache<String, Map<String,Cell>>(){

			/**
			 * 
			 */
			private static final long serialVersionUID = -1497598693844155855L;

			@Override
			protected Map<String, Cell> getValue(String k) {
				return new HashMap<String, Cell>();
			}
			
		};
		
		Iterator<String> headingIterator = headingValues.keySet().iterator();
		while (headingIterator.hasNext()){
			String heading = headingIterator.next();
			int indexOfDot = heading.indexOf('.');
			
			if (indexOfDot >= 0){
				String referredFieldName = StringUtil.underscorize(heading.substring(0, indexOfDot) + "Id"); 
				newHeadingValues.get(referredFieldName).put(heading.substring(indexOfDot+1), headingValues.get(heading));
			}else {
				String fieldName = StringUtil.underscorize(heading);
				Object value = getCellValue(headingValues.get(heading),reflector.getFieldGetter(fieldName).getReturnType());
				where.add(new Expression(StringUtil.underscorize(heading),Operator.EQ,value));
			}
		}
		for (String referenceFieldName: newHeadingValues.keySet()){
			Class<? extends Model> referredModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(reflector.getFieldGetter(referenceFieldName)));
			Model referred = getModel(ModelReflector.instance(referredModelClass), newHeadingValues.get(referenceFieldName));
			if (referred != null){
				where.add(new Expression(referenceFieldName,Operator.EQ,referred.getId()));
			}
		}
		
		List<? extends Model> m = new Select().from(reflector.getModelClass()).where(where).execute();
		if (m.size() == 1){
			return m.get(0);
		}else if (m.size() == 0){
			return null;
		}else {
			throw new RuntimeException("Unique Record not found in " + reflector.getTableName() + " for " + where.getRealSQL());
		}
	}


}
