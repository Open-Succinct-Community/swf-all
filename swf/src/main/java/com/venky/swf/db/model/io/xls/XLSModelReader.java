package com.venky.swf.db.model.io.xls;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelReader;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class XLSModelReader<M extends Model> extends XLSModelIO<M> implements ModelReader<M,Row>{

	public XLSModelReader(Class<M> modelClass) {
		super(modelClass);
	} 
	
	@Override
	public List<M> read(InputStream source) throws IOException{
		return read(source,getBeanClass().getSimpleName());
	}
	
	public List<M> read(Sheet sheet){
        List<M> records = new ArrayList<M>();
        if (sheet == null){
        	return records;
        }
        
		Iterator<Row> rowIterator = sheet.iterator();
        Row header = rowIterator.hasNext() ? rowIterator.next() : null;
        if (header == null){
        	return records;
        }
        
        Map<String,Integer> headingIndexMap = headingIndexMap(sheet);
        while (rowIterator.hasNext()){
        	Row row = rowIterator.next();
        	M m = read(row,headingIndexMap);
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
	
	private Map<String,Integer> headingIndexMap(Sheet sheet){
        Map<String,Integer> headingIndexMap = new SequenceMap<String, Integer>();
		if (sheet == null || sheet.getLastRowNum() < 0){
			return headingIndexMap; 
		}
		Row header = sheet.getRow(0);
        for (int i = 0 ; i < header.getLastCellNum() ; i ++ ){
            headingIndexMap.put(header.getCell(i).getStringCellValue(), i);
        }
        return headingIndexMap;
	}
	

	
	@Override
	public M read(Row source) {
    	Map<String,Integer> headingIndexMap = headingIndexMap(source.getSheet());
		return read(source , headingIndexMap);
	}

	private M read(Row source,Map<String, Integer> headingIndexMap){
		M m = createInstance();
		copyRowValuesToBean(m, source, headingIndexMap);
		return Database.getTable(getBeanClass()).getRefreshed(m);
	}


	protected void copyRowValuesToBean(M m, Row row, Map<String, Integer> headingIndexMap) {
		String[] heading = headingIndexMap.keySet().toArray(new String[]{});

		ModelReflector<M> ref = getReflector();
		SequenceSet<String> handledReferenceFields = new SequenceSet<String>();
		
		for (int i = 0; i < heading.length; i++) {
			Method getter = getGetter(row,heading[i],headingIndexMap);
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
					Integer headingIndex = headingIndexMap.get(field);
					if (headingIndex == null){
						continue;
					}
					Cell cell1 = row.getCell(headingIndex);
					if (cell1 == null){
						continue;
					}
					if (cell1.getCellType() != Cell.CELL_TYPE_BLANK) {
						referenceFieldsPassed = true;
					}
					fieldValues.put(field.substring(field.indexOf('.')+1), cell1);
				}

				Model referredModel = getModel(referredModelReflector,fieldValues);
				if (referredModel == null && referenceFieldsPassed){
					handleInvalidReference(m,row,fieldName, referredModelClass, fieldValues);
				}else if (referredModel != null){
					value = referredModel.getId();
				}else {
					value = null;
				}
			} else if (type == GetterType.FIELD_GETTER) {
				value = getCellValue(cell,getter.getReturnType());
			}

			try {
				set(m,fieldName,value);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot set " + heading[i] + " as "
						+ value + " of Class " + value.getClass().getName(), e);
			}
		}

	}
	
	

	protected void handleInvalidReference(M model, Row xlsRow, String fieldName, Class<? extends Model> referredModelClass, Map<String,Cell> fieldValues){
		throw new RuntimeException(referredModelClass.getSimpleName() + " not found for passed information " + fieldValues.toString());
	}
	
	protected void set(M m , String fieldName, Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		getReflector().set(m, fieldName,value);
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

	@Override
	public List<M> read(InputStream in, String rootElementName)
			throws IOException {

		Workbook book = new HSSFWorkbook(in);
		Sheet sheet = book.getSheet(StringUtil.pluralize(rootElementName));
		return read(sheet);
	}
}
