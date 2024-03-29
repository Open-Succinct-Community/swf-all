package com.venky.swf.db.model.io.xls;

import com.venky.cache.Cache;
import com.venky.core.collections.SequenceMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.NumericConverter;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelWriter;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.util.WordWrapUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XLSModelWriter<M extends Model> extends XLSModelIO<M> implements ModelWriter<M,Row>{
	boolean parentIdExposed = true;
	public void setParentIdExposed(boolean parentIdExposed){
		this.parentIdExposed = parentIdExposed;
	}
	public boolean isParentIdExposed() {
		return this.parentIdExposed;
	}
	private final SequenceMap<String, Class<? extends Model>> referedModelMap = new SequenceMap<String,Class<? extends Model>>();
	private final SequenceMap<String, SequenceSet<String>> referredModelFieldsToExport = new SequenceMap<String, SequenceSet<String>>();
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
	
	@Override
	public void write(List<M> records, OutputStream os, List<String> fields) throws IOException {
		write(records,os,fields, new HashSet<Class<? extends Model>>(), new HashMap<Class<? extends Model>,List<String>>());
	}

	@Override
	public void write(List<M> records, Row into, List<String> fields) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(List<M> records, OutputStream os, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered,
			Map<Class<? extends Model>, List<String>> templateFields) throws IOException {
		write(records,os,fields,parentsAlreadyConsidered,null,templateFields);
	}

	@Override
	public void write(List<M> records, Row into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<String>> templateFields) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(List<M> records, OutputStream os, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<Class<? extends Model>>> childrenToBeConsidered, Map<Class<? extends Model>, List<String>> templateFields) throws IOException {
		Workbook wb = new XSSFWorkbook();
		write(records,wb,fields,parentsAlreadyConsidered, templateFields);
		wb.write(os);

	}

	@Override
	public void write(List<M> records, Row into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<Class<? extends Model>>> childrenToBeConsidered, Map<Class<? extends Model>, List<String>> templateFields) throws IOException {
		throw new UnsupportedOperationException();
	}


	public Sheet createSheet(Workbook book, String sheetName){
		Sheet sheet = book.createSheet(sheetName);
		sheet.setAutobreaks(false);

		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setLandscape(true);
		printSetup.setFitWidth((short)1);
		printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);

		return sheet;
	}
	public void write(List<M> records, Workbook wb, List<String> fields, Set<Class<? extends Model>> ignoreParents, Map<Class<? extends Model>, List<String>> childfields) {
		write(records, wb, StringUtil.pluralize(getBeanClass().getSimpleName()), fields, ignoreParents, childfields);
	}
	public void write(List<M> records, Workbook wb, String sheetName, List<String> fields, Set<Class<? extends Model>> ignoreParents, Map<Class<? extends Model>, List<String>> childfields) {
		Sheet sheet = createSheet(wb,sheetName);
		StyleHelper helper = new StyleHelper(wb);
		
    	Bucket rowNum = new Bucket(START_ROW); 
    	Bucket columnNum = new Bucket(START_COLUMN);
    	Row header = sheet.createRow(rowNum.intValue());

    	Iterator<String> fi = fields.iterator();
		while (fi.hasNext()){
			String fieldName = fi.next();
			if (referedModelMap.get(fieldName) == null){
				if (fieldName.equalsIgnoreCase("ID")){
					fieldName = "ORIGINAL_ID";
				}
				createCell(sheet, header, columnNum, StringUtil.camelize(fieldName), helper.headerStyle);
			}else {
				for (String headerField : referredModelFieldsToExport.get(fieldName)){
					createCell(sheet, header, columnNum, headerField, helper.headerStyle);
				}
			}
		}
		sheet.setRepeatingColumns(new CellRangeAddress(rowNum.intValue(), rowNum.intValue(), 0, columnNum.intValue() -1 ));
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
    		write(m,r,fields,ignoreParents,childfields,helper);
    	}

    	
	}

	public void write(M m, Row r, List<String> fields) {
		write(m,r,fields,new HashSet<Class<? extends Model>>(), new HashMap<Class<? extends Model>,List<String>>());
	}
	@Override
	public void write(M record, Row into, List<String> fields, Set<Class<?extends  Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<String>> templateFields) {
		write(record,into,fields,parentsAlreadyConsidered,null,templateFields);
	}

	@Override
	public void write(M record, Row into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<Class<? extends Model>>> childrenToBeConsidered, Map<Class<? extends Model>, List<String>> templateFields) {
		write (record,into,fields,parentsAlreadyConsidered, templateFields,null);
	}

	@Override
	public void write(M record, Row into, List<String> fields, Set<Class<? extends Model>> parentsAlreadyConsidered, Map<Class<? extends Model>, List<Class<? extends Model>>> childrenToBeConsidered,
					  Map<Class<? extends Model>, List<String>> templateFields, boolean includeNulls) {
		write (record,into,fields,parentsAlreadyConsidered, templateFields,null);
	}


	public void alignTop(CellStyle style){
		style.setAlignment(HorizontalAlignment.LEFT);
		style.setVerticalAlignment(VerticalAlignment.TOP);
	}
	public void center(CellStyle style){
		style.setAlignment(HorizontalAlignment.LEFT);
		style.setVerticalAlignment(VerticalAlignment.TOP);
	}
	private class StyleHelper {
		CellStyle integerStyle;
		CellStyle decimalStyle;
		CellStyle dateStyle;
		CellStyle stringStyle;
		CellStyle headerStyle;
		Font font;
		private Font createDefaultFont(Workbook wb){
			Font font = wb.createFont();
			font.setFontName("Courier New");
			font.setFontHeightInPoints((short)(CHARACTER_HEIGHT_IN_POINTS));
			return font;
		}

		private StyleHelper(Workbook wb){
			font = createDefaultFont(wb);
			
			headerStyle = wb.createCellStyle();
			headerStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setFont(font);
			headerStyle.setWrapText(true);
			center(headerStyle);
			

			CreationHelper createHelper = wb.getCreationHelper();
			decimalStyle = wb.createCellStyle();
			decimalStyle.setDataFormat(createHelper.createDataFormat().getFormat("#.0##"));
			decimalStyle.setFont(font);
			center(decimalStyle);

			integerStyle = wb.createCellStyle();
			integerStyle.setDataFormat(createHelper.createDataFormat().getFormat("0"));
			integerStyle.setFont(font);
			center(integerStyle);
			
			dateStyle = wb.createCellStyle();
			dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy"));
			dateStyle.setFont(font);
			center(dateStyle);
			
			stringStyle = wb.createCellStyle();
			stringStyle.setWrapText(true);
			stringStyle.setFont(font);
			alignTop(stringStyle);
		}
	}
	@Override
	public void writeSimplified(M record, Row into, List<String> fields, Set<String> parentsAlreadyConsidered, Map<String, List<String>> childrenToBeConsidered, Map<String, List<String>> templateFields) {
		writeSimplified(record,into,fields,parentsAlreadyConsidered, templateFields,(StyleHelper)null);
	}

	@Override
	public void writeSimplified(M record, Row into, List<String> fields, Set<String> parentsAlreadyConsidered, Map<String, List<String>> considerChildren, Map<String, List<String>> templateFields, boolean includeNulls) {
		writeSimplified(record,into,fields,parentsAlreadyConsidered, templateFields,(StyleHelper)null);
	}

	private void write(M m, Row r, List<String> fields, Set<Class<?extends  Model>> parentsAlreadyConsidered,
					   Map<Class<? extends Model>, List<String>> templateFields,StyleHelper helper) {
		Set<String> simplifiedParentsConsidered = new HashSet<>();
		parentsAlreadyConsidered.forEach(c->simplifiedParentsConsidered.add(c.getSimpleName()));

		Map<String,List<String>> simplifiedConsiderChildren = new Cache<String, List<String>>() {
			@Override
			protected List<String> getValue(String s) {
				return new SequenceSet<>();
			}
		};
		Map<String,List<String>> simplifiedTemplateFields = new Cache<String, List<String>>() {
			@Override
			protected List<String> getValue(String s) {
				return new SequenceSet<>();
			}
		};

		templateFields.forEach((c,fl)->{
			for (String f: fl){
				simplifiedTemplateFields.get(c.getSimpleName()).add(f);
			}
		});
		writeSimplified(m,r,fields,simplifiedParentsConsidered,simplifiedTemplateFields,helper);
	}
	private void writeSimplified(M m, Row r, List<String> fields, Set<String> ignoreParents,
					   Map<String, List<String>> childfields,StyleHelper helper) {
		if (helper == null){
			helper = new StyleHelper(r.getSheet().getWorkbook());
		}
		Iterator<String> fi = fields.iterator();
		ModelReflector<M> ref = getReflector();
		Bucket columnNum = new Bucket(START_COLUMN);
		while (fi.hasNext()){
			String f = fi.next();
			Object value = ref.get(m, f);
			if (referedModelMap.get(f) != null){
				for (String cf: referredModelFieldsToExport.get(f) ){
					writeNextColumn(r, columnNum, getValue(m,cf), helper.integerStyle, helper.decimalStyle,helper.dateStyle,helper.stringStyle);
				}
			}else {
				writeNextColumn(r, columnNum, value, helper.integerStyle,helper.decimalStyle, helper.dateStyle, helper.stringStyle);
			}
		}
		//TODO Need to take care of childen models here.!!
	}
	

	
	protected Cell writeNextColumn(Row r, Bucket columnNum , Object value, CellStyle integerStyle, CellStyle decimalStyle,CellStyle dateStyle,CellStyle stringStyle){
		Cell cell = null; 
		if (!ObjectUtil.isVoid(value)){
			Class<?> colClass = value.getClass();
			if (isNumeric(colClass)){
				if (NumericConverter.class.isAssignableFrom(Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(colClass).getTypeConverter().getClass())){
					cell = createCell(r.getSheet(),r,columnNum,value,decimalStyle);
				}else {
					cell = createCell(r.getSheet(),r,columnNum,value,integerStyle);
				}
            }else if (isDate(colClass)) {
            	cell = createCell(r.getSheet(), r, columnNum, value, dateStyle);
            }else if (isBoolean(colClass)) {
				TypeConverter<Boolean> converter = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(Boolean.class).getTypeConverter();

                cell = createCell(r.getSheet(), r, columnNum, converter.toString(value), stringStyle);
            }else{
                cell = createCell(r.getSheet(),r , columnNum, value , stringStyle);
            }
			
		}else {
			cell = createCell(r.getSheet(), r, columnNum, value, null);
		}
		return cell;
	}

	private static final int MAX_COLUMN_LENGTH = 30 ;
	public static final int CHARACTER_WIDTH = 300 ; // earlier 293; fixed for ms excel.
	private static final int CHARACTER_HEIGHT_IN_POINTS = 10 ;
	
	private int getColumnWidth(Sheet sheet,int columnIndex){
		if (sheet.getNumMergedRegions() == 0){
			return sheet.getColumnWidth(columnIndex);
		}else {
			boolean columnMergedInSomeRow = false; 
			Set<Integer> skipRows = new HashSet<Integer>();
			for (int i = 0 ; i < sheet.getNumMergedRegions() ; i ++ ){
				CellRangeAddress add = sheet.getMergedRegion(i);
				if (add.getFirstColumn() <= columnIndex && columnIndex <= add.getLastColumn() && (add.getFirstColumn() != add.getLastColumn()) ){
					columnMergedInSomeRow = true;
					for (int r = add.getFirstRow() ; r <= add.getLastRow() ; r ++ ){
						skipRows.add(r);
					}
				}
			}
			if (!columnMergedInSomeRow){
				return sheet.getColumnWidth(columnIndex);
			}else {
				int maxSkippedRow = Collections.max(skipRows);
				if (sheet.getPhysicalNumberOfRows() -1 <= maxSkippedRow + 1){
					return 0;
				}else {
					return sheet.getColumnWidth(columnIndex);	
				}
			}
		}
	}
	
	private void fixCellDimensions(Sheet sheet,Row row,Bucket columnNum,CellStyle style, Object value, int maxColumnLength){
		int currentColumnWidth = getColumnWidth(sheet,columnNum.intValue());
		String sValue = Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(value.getClass()).getTypeConverter().toString(value);
		int currentValueLength = sValue.length() ;
		int numRowsRequiredForCurrentValue = WordWrapUtil.getNumRowsRequired(sValue,maxColumnLength);
		Font font = sheet.getWorkbook().getFontAt(style.getFontIndexAsInt());
		
		if (currentColumnWidth < maxColumnLength * CHARACTER_WIDTH){
			int currentValueWidth = (currentValueLength + 1)* CHARACTER_WIDTH; 
			currentColumnWidth = Math.min(Math.max(currentValueWidth,currentColumnWidth), maxColumnLength * CHARACTER_WIDTH);
			sheet.setColumnWidth(columnNum.intValue(), currentColumnWidth);
		}
		row.setHeightInPoints(Math.max(row.getHeightInPoints() , getRowHeightInPoints(numRowsRequiredForCurrentValue,font)));
	}
	
	public int getRowHeightInPoints(int numRows,Font font){
		return numRows*((font == null ? CHARACTER_HEIGHT_IN_POINTS : font.getFontHeightInPoints()) + 4) + 5;
	}
	public Cell createCell(Sheet sheet, Row row, Bucket columnNum , Object  value, CellStyle style){
		return createCell(sheet,row,columnNum,value,style,MAX_COLUMN_LENGTH);	
	}
	public Cell createCell(Sheet sheet, Row row, Bucket columnNum , Object  value, CellStyle style,int maxColumnLength){
		Cell cell = row.createCell(columnNum.intValue());
		if (style != null){
			cell.setCellStyle(style);
		}
		if (value != null){
			Class<?> colClass = value.getClass();
			if (style != null && style.getWrapText()){
				fixCellDimensions(sheet, row, columnNum, style,value, maxColumnLength);
			}
			if (isNumeric(colClass)){
				cell.setCellValue(Double.valueOf(String.valueOf(value)));
			}else if (isDate(colClass)){
				cell.setCellValue((Date)value);
			}else if (isBoolean(colClass)){
				cell.setCellValue(Boolean.valueOf(String.valueOf(value)));
			}else {
				cell.setCellValue(Database.getJdbcTypeHelper(getReflector().getPool()).getTypeRef(colClass).getTypeConverter().toString(value));
			}
		}
		columnNum.increment();
		return cell;
	}
	
}
