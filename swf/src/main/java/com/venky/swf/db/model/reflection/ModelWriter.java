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

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
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
		numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));
		
		CellStyle dateStyle = wb.createCellStyle();
		dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy"));
	
		CellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
		
		Iterator<String> fi = fields.iterator();
    	
    	HashMap<String, Class<? extends Model>> referedModelMap = new HashMap<String,Class<? extends Model>>();
    	int rowNum = START_ROW; 
    	int columnNum = START_COLUMN;
    	Row header = sheet.createRow(rowNum);
    	while(fi.hasNext()){
    		String fieldName = fi.next();
    		Method getter = ref.getFieldGetter(fieldName);
			Method referredModelGetter = ref.getReferredModelGetterFor(getter);
			Cell headerCell = header.createCell(columnNum);
			headerCell.setCellStyle(headerStyle);
			if (referredModelGetter != null ){
				headerCell.setCellValue(referredModelGetter.getName().substring("get".length()));
				referedModelMap.put(fieldName, ref.getReferredModelClass(referredModelGetter));
			}else {
				headerCell.setCellValue(StringUtil.camelize(fieldName));
			}
			columnNum++;
    	}
    	
    	for (M m: records){
    		rowNum++;
    		columnNum = START_COLUMN;
    		Row r = sheet.createRow(rowNum);
    		fi = fields.iterator();
    		while (fi.hasNext()){
    			String f = fi.next();
    			Object value = ref.get(m, f);
    			String referredTableDescription = null ;
    			if (value != null ){
        			TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(value.getClass()).getTypeConverter();
        			if (referedModelMap.get(f) != null){
        				Class<? extends Model> referredModelClass = referedModelMap.get(f);
        				ModelReflector<?> referredModelReflector = ModelReflector.instance(referredModelClass);
    					Model referred = Database.getTable(referredModelClass).get(((Number)converter.valueOf(value)).intValue());
    					
    					value = referred.getRawRecord().get(referredModelReflector.getDescriptionColumn());
    					referredTableDescription = StringUtil.valueOf(referred.getRawRecord().get(referredModelReflector.getDescriptionColumn()));
        			}
    			}

    			if (!ObjectUtil.isVoid(value)){
    				Class<?> colClass = value.getClass();
    				Cell cell = r.createCell(columnNum);
    				if (isNumeric(colClass)){
    					if (referredTableDescription != null){
    						cell.setCellValue(referredTableDescription);
						}else {
	                        cell.setCellValue(Double.valueOf(String.valueOf(value)));
	                        cell.setCellStyle(numberStyle);
    					}
                    }else if (isDate(colClass)) {
                        cell.setCellValue((Date)value);
                        cell.setCellStyle(dateStyle);
                    }else if (isBoolean(colClass)) {
                        cell.setCellValue((Boolean)value);
                    }else{
                        cell.setCellValue(String.valueOf(value));
                    }
    			}
    			columnNum ++;
			}
    	}

	}

}
