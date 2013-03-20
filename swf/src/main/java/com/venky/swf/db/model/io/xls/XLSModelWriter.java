package com.venky.swf.db.model.io.xls;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.NumericConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;

public class XLSModelWriter<M extends Model> extends XLSModelIO<M> implements ModelWriter<M,Row>{
	
	private final HashMap<String, Class<? extends Model>> referedModelMap = new HashMap<String,Class<? extends Model>>();
	private final HashMap<String, SequenceSet<String>> referredModelFieldsToExport = new HashMap<String, SequenceSet<String>>();
	public XLSModelWriter(Class<M> modelClass){
		this(modelClass,false);
	}
	public XLSModelWriter(Class<M> modelClass,boolean reportMode){
		super(modelClass);
		this.reportMode = reportMode;
    	ModelReflector<M> ref = getReflector();
    	
    	Iterator<String> fi = ref.getFields().iterator();
    	while(fi.hasNext()){
    		String fieldName = fi.next();
    		Method getter = ref.getFieldGetter(fieldName);
			Method referredModelGetter = ref.getReferredModelGetterFor(getter);
			
			if (referredModelGetter != null ){
				Class<? extends Model> referredModelClass = ref.getReferredModelClass(referredModelGetter);
				ModelReflector<? extends Model> referredModelReflector = ModelReflector.instance(referredModelClass);
				String baseFieldHeading = referredModelGetter.getName().substring("get".length());
				SequenceSet<String> fieldsToExport = new SequenceSet<String>();
				referedModelMap.put(fieldName,referredModelClass);
				if (!ref.isFieldSettable(fieldName)){
					fieldsToExport.add(baseFieldHeading + "." + StringUtil.camelize(referredModelReflector.getDescriptionField()));
				}else {
					loadFieldsToExport(fieldsToExport, baseFieldHeading, referredModelReflector);
				}
				referredModelFieldsToExport.put(fieldName,fieldsToExport);
			}
			
    	}
	}
	private boolean reportMode = false;
	
	private static final int START_ROW = 0; 
	private static final int START_COLUMN = 0;
	
	public void write(List<M> records, OutputStream os, List<String> fields) throws IOException {
		Workbook wb = new HSSFWorkbook();
		write(records,wb,fields);
		wb.write(os);
	}
	public void write(List<M> records, Workbook wb, List<String> fields) {
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		
		String sheetName = StringUtil.pluralize(getBeanClass().getSimpleName());
		Sheet sheet = wb.createSheet(sheetName);

    	Bucket rowNum = new Bucket(START_ROW); 
    	Bucket columnNum = new Bucket(START_COLUMN);
    	Row header = sheet.createRow(rowNum.intValue());

    	Iterator<String> fi = fields.iterator();
		while (fi.hasNext()){
			String fieldName = fi.next();
			if (referedModelMap.get(fieldName) == null){
				Cell headerCell = header.createCell(columnNum.intValue());
				headerCell.setCellStyle(headerStyle);
				headerCell.setCellValue(StringUtil.camelize(fieldName));
				columnNum.increment();
			}else {
				for (String headerField : referredModelFieldsToExport.get(fieldName)){
					Cell headerCell = header.createCell(columnNum.intValue());
					headerCell.setCellStyle(headerStyle);
					headerCell.setCellValue(headerField);
					columnNum.increment();
				}
			}
		}
    	
    	
    	for (int i = 0 ; i < records.size() ; i ++ ){
    		M m = records.get(i);
    		rowNum.increment();
    		Row r = sheet.createRow(rowNum.intValue());
    		if (reportMode){
    			ModelReflector<M> reflector = getReflector();
        		if (i > 0){
	    			M prev = records.get(i-1);
	    			M clone = m.cloneProxy();
	    			for (int fieldNumber = 0 ; fieldNumber < fields.size() ; fieldNumber ++ ){
	    				String field = fields.get(fieldNumber);
	    				Object prevFieldValue = reflector.get(prev,field);
	    				Object currentFieldValue = reflector.get(m, field);
	    				if (ObjectUtil.equals(prevFieldValue, currentFieldValue)){
	    					reflector.set(clone, field, null);
	    				}else {
	    					break;
	    				}
	    			}
	    			m = clone;
	    		}
    		}
    		write(m,r,fields);
    	}
    	
    	for (int i = 0 ; i < columnNum.intValue() ; i ++ ){
    		sheet.autoSizeColumn(i);
    		sheet.setColumnWidth(i, Math.max(Math.min(sheet.getColumnWidth(i), 40 * 256),5 * 256));
    	}
	}

	@Override
	public void write(M m, Row r, List<String> fields) {
		Workbook wb = r.getSheet().getWorkbook();
		
		CreationHelper createHelper = wb.getCreationHelper();
		CellStyle decimalStyle = wb.createCellStyle();
		decimalStyle.setDataFormat(createHelper.createDataFormat().getFormat("#.0##"));
		
		CellStyle integerStyle = wb.createCellStyle();
		integerStyle.setDataFormat(createHelper.createDataFormat().getFormat("0"));
		
		CellStyle dateStyle = wb.createCellStyle();
		dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy"));
		
		CellStyle stringStyle = wb.createCellStyle();
		stringStyle.setWrapText(true);
	
		Iterator<String> fi = fields.iterator();
		ModelReflector<M> ref = getReflector();
		Bucket columnNum = new Bucket(START_COLUMN);
		while (fi.hasNext()){
			String f = fi.next();
			Object value = ref.get(m, f);
			if (referedModelMap.get(f) != null){
				for (String cf: referredModelFieldsToExport.get(f) ){
					writeNextColumn(r, columnNum, getValue(m,cf), integerStyle, decimalStyle,dateStyle,stringStyle);
					columnNum.increment();
				}
			}else {
				writeNextColumn(r, columnNum, value, integerStyle,decimalStyle, dateStyle,stringStyle);
				columnNum.increment();
			}
		}
	}
	

	
	protected void writeNextColumn(Row r, Bucket columnNum , Object value, CellStyle integerStyle, CellStyle decimalStyle,CellStyle dateStyle,CellStyle stringStyle){
		if (!ObjectUtil.isVoid(value)){
			Class<?> colClass = value.getClass();
			Cell cell = r.createCell(columnNum.intValue());
			if (isNumeric(colClass)){
                cell.setCellValue(Double.valueOf(String.valueOf(value)));
                
				if (NumericConverter.class.isAssignableFrom(Database.getJdbcTypeHelper().getTypeRef(colClass).getTypeConverter().getClass())){
					cell.setCellStyle(decimalStyle);	
				}else {
					cell.setCellStyle(integerStyle);
				}
            }else if (isDate(colClass)) {
                cell.setCellValue((Date)value);
                cell.setCellStyle(dateStyle);
            }else if (isBoolean(colClass)) {
                cell.setCellValue((Boolean)value);
            }else{
                cell.setCellValue(Database.getJdbcTypeHelper().getTypeRef(colClass).getTypeConverter().toString(value));
                cell.setCellStyle(stringStyle);
            }
		}
	}

}
