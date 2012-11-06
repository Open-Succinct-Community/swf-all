package com.venky.swf.db.model.reflection;

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
import com.venky.swf.db.model.Model;

public class ModelWriter<M extends Model> extends ModelIO<M>{
	
	Class<M> modelClass = null;
	ModelReflector<M> ref = null; 
	public ModelWriter(Class<M> modelClass){
		super(modelClass);
		this.modelClass = modelClass;
		this.ref = ModelReflector.instance(modelClass); 
	}
	
	public ModelReflector<M> getReflector(){
		return ref;
	}
	public void write (List<M> records,OutputStream os) throws IOException{
		Workbook wb = new HSSFWorkbook();
		write(records,wb);
		wb.write(os);
	}
	public void write (List<M> records,Workbook wb){
		write(records,wb,ref.getFields());
	}
	private static final int START_ROW = 0; 
	private static final int START_COLUMN = 0;
	
	public void write (List<M> records,Workbook wb, List<String> fields){
		CreationHelper createHelper = wb.getCreationHelper();
		Sheet sheet = wb.createSheet(StringUtil.pluralize(ref.getModelClass().getSimpleName()));
		
		CellStyle numberStyle = wb.createCellStyle();
		numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#.###"));
		
		CellStyle dateStyle = wb.createCellStyle();
		dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy"));
	
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		Iterator<String> fi = fields.iterator();
    	
    	HashMap<String, Class<? extends Model>> referedModelMap = new HashMap<String,Class<? extends Model>>();
    	HashMap<String, SequenceSet<String>> referredModelFieldsToExport = new HashMap<String, SequenceSet<String>>();
    	
    	int rowNum = START_ROW; 
    	Bucket columnNum = new Bucket(START_COLUMN);
    	Row header = sheet.createRow(rowNum);
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
    	
    	for (M m: records){
    		rowNum++;
    		columnNum = new Bucket(START_COLUMN);
    		Row r = sheet.createRow(rowNum);
    		fi = fields.iterator();
    		while (fi.hasNext()){
    			String f = fi.next();
    			Object value = ref.get(m, f);
    			if (referedModelMap.get(f) != null){
    				for (String cf: referredModelFieldsToExport.get(f) ){
    					writeNextColumn(r, columnNum, getValue(m,cf), numberStyle, dateStyle);
    					columnNum.increment();
    				}
    			}else {
    				writeNextColumn(r, columnNum, value, numberStyle, dateStyle);
    				columnNum.increment();
    			}
			}
    	}
    	
    	for (int i = 0 ; i < columnNum.intValue() ; i ++ ){
    		sheet.autoSizeColumn(i);
    	}

	}
	
	private void writeNextColumn(Row r, Bucket columnNum , Object value, CellStyle numberStyle,CellStyle dateStyle){
		if (!ObjectUtil.isVoid(value)){
			Class<?> colClass = value.getClass();
			Cell cell = r.createCell(columnNum.intValue());
			if (isNumeric(colClass)){
                cell.setCellValue(Double.valueOf(String.valueOf(value)));
                cell.setCellStyle(numberStyle);
            }else if (isDate(colClass)) {
                cell.setCellValue((Date)value);
                cell.setCellStyle(dateStyle);
            }else if (isBoolean(colClass)) {
                cell.setCellValue((Boolean)value);
            }else{
                cell.setCellValue(Database.getJdbcTypeHelper().getTypeRef(colClass).getTypeConverter().toString(value));
            }
		}
	}
	

}
