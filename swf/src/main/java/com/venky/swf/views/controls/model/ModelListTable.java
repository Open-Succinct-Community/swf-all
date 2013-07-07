package com.venky.swf.views.controls.model;

import static com.venky.core.log.TimerStatistics.Timer.startTimer;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.model.ModelAwareness.OrderBy;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.model.FieldUIMetaProvider;

public class ModelListTable<M extends Model> extends Table{

	private static final long serialVersionUID = 3569299125414888353L;

	private Map<String,Integer> maxFieldWidth = new HashMap<String,Integer>() ;
	
	private FieldUIMetaProvider metaprovider;
	public FieldUIMetaProvider getMetaprovider() {
		return metaprovider;
	}

	private ModelAwareness modelAwareness = null;
	public ModelAwareness getModelAwareness() {
		return modelAwareness;
	}

	public ModelListTable(Path path, String[] includeFields , FieldUIMetaProvider metaProvider) {
		addClass("hfill");
		addClass("tablesorter");
		this.modelAwareness = new ModelAwareness(path,includeFields);
		this.metaprovider = metaProvider;
	}
	
	public void addRecords(List<M> records){
        Row header = createHeader();
        BitSet showAction = addHeadingsForLineLevelActions(header);
        
        addHeadings(header);
        
        for (M record : records) {
        	addRecordToTable(record,showAction);        
        }
        
        int numActionsRemoved = 0 ;
        int numActions = getSingleRecordActions().size();
        for (int i = 0 ; i < numActions ; i ++ ){
        	if (!showAction.get(i)){
        		removeColumn(i-numActionsRemoved);
        		numActionsRemoved ++; // Once a column is removed the indexes are shifted left. 
        	}
        }

        
        numActions -= numActionsRemoved;
        OrderBy orderby = modelAwareness.getOrderBy();
        
        int orderByFieldIndex = getMetaprovider().isFieldVisible(orderby.field) ? getIncludedVisibleFields().indexOf(orderby.field) : -1;
        if (orderByFieldIndex >= 0 ){
        	setProperty("sortby", numActions + orderByFieldIndex);
        	setProperty("order", orderby.sortDirection());
        }
        setWidths(header,numActions);

	}
	private List<String> getIncludedVisibleFields() {
		List<String> visibleFields = new ArrayList<String>();
		for (String field: getIncludedFields()){
			if (getMetaprovider().isFieldVisible(field)){
				visibleFields.add(field);
			}
		}
		return visibleFields;
	}
	private List<String> getIncludedFields() {
		return getModelAwareness().getIncludedFields();
	}

	protected void addHeadings(Row headerRow){
    	List<String> indexedFields = modelAwareness.getReflector().getIndexedFields();
    	
        for (String fieldName : getIncludedFields()) {
        	if (!getMetaprovider().isFieldVisible(fieldName)){
        		continue;
        	}
        	String literal = modelAwareness.getFieldLiteral(fieldName);
        	Column column = headerRow.createColumn();
        	column.setText(literal);
        	if (indexedFields.contains(fieldName)){
            	column.addClass("indexed");
        	}
        	Integer currentMaxFieldWidth = maxFieldWidth.get(fieldName);
        	if (currentMaxFieldWidth == null || currentMaxFieldWidth < literal.length()){
        		maxFieldWidth.put(fieldName,literal.length());
        	}
        }
    }

	
    protected void setWidths(Row header,int numActions){
    	Timer timer = startTimer("Setting widths",Config.instance().isTimerAdditive());
    	try { 
    		_setWidths(header, numActions);
    	}finally {
    		timer.stop();
    	}
    }
    protected void _setWidths(Row header,int numActions){
    	Map<String,Integer> fieldWidthMap = null; //suggestedFieldWidth; 
    	
    	if (fieldWidthMap == null){
    		fieldWidthMap = maxFieldWidth;
    	}
    	
    	List<Column> columns = new ArrayList<Column>();
    	Bucket total = new Bucket();
    	Control.hunt(header,Column.class,columns);

    	int i = numActions;

    	total.increment(numActions);
    	final int fieldOffset = 3 ;;// Extra for sorting icon space.
    	for (String field: fieldWidthMap.keySet()){
    		total.increment(fieldWidthMap.get(field) + fieldOffset);
    	}
    	
    	
    	for (String field: getIncludedFields()){
    		if (!getMetaprovider().isFieldVisible(field)){
    			continue;
    		}
    		int currentFieldWidth = fieldWidthMap.get(field) + fieldOffset;
    		long pctWidth = (int)( (currentFieldWidth * 100.0) / total.doubleValue() );
    		Column column = columns.get(i);
    		column.setProperty("width",  pctWidth +"%");
    		i++;
    	}
    	
    }
	protected BitSet addHeadingsForLineLevelActions(Row header) {
		BitSet showAction = new BitSet();
        int numActions = getSingleRecordActions().size(); 
        for (int i = 0 ; i < numActions ; i ++ ){
        	Control action = header.createColumn();
            action.setProperty("width", "1%");
            showAction.clear(i);
        }
        return showAction;
	}
	protected void addLineLevelActions(Row row, M record, BitSet showAction) {
		Timer timer = startTimer("Adding Line Level Actions",Config.instance().isTimerAdditive());
		try {
			_addLineLevelActions(row, record, showAction);
		}finally {
			timer.stop();
		}
	}
	
	protected List<Method> getSingleRecordActions(){ 
		return modelAwareness.getSingleRecordActions();
	}
	protected void _addLineLevelActions(Row row, M record, BitSet showAction) {
    	List<Method> singleRecordActions = getSingleRecordActions();
        for (int actionIndex = 0 ; actionIndex < singleRecordActions.size() ; actionIndex ++ ){
        	Method m = singleRecordActions.get(actionIndex);
        	Column actionLinkCell = row.createColumn();
        	Link singleRecordActionLink = modelAwareness.createSingleRecordActionLink(m, record);
        	if (singleRecordActionLink != null){
        		actionLinkCell.addControl(singleRecordActionLink);
        		showAction.set(actionIndex);
        	}
        }
	}
	protected void addFields(Row row, M record){
		Timer addingFields = startTimer("Adding Fields",Config.instance().isTimerAdditive());
		try {
			_addFields(row, record);
		}finally {
			addingFields.stop();
		}
	}
	protected Control getControl(String controlName, String fieldName, M record){
		ModelReflector<M> reflector = modelAwareness.getReflector();
    	Method getter = reflector.getFieldGetter(fieldName);
		TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
        Control control = null;
        
        if (InputStream.class.isAssignableFrom(getter.getReturnType())){
        	FileTextBox ftb = (FileTextBox)modelAwareness.getInputControl(controlName,fieldName, record,null);
        	String contentName = reflector.getContentName(record, fieldName);
			if (reflector.getContentSize(record, fieldName) != 0){
				ftb.setStreamUrl(modelAwareness.getPath().controllerPath()+"/view/"+record.getId(),contentName);
            	control = ftb.getStreamLink();
			}else {
				control = new Label("No Attachment");
			}
        }else {
            Object value = reflector.get(record,fieldName);
            String sValue = converter.toString(value);
            if (reflector.isFieldPassword(fieldName)){
            	sValue = sValue.replaceAll(".", "\\*");
            }
            String parentDescription = modelAwareness.getParentDescription(getter, record) ;
            if (!ObjectUtil.isVoid(parentDescription)){
            	Object parentId = value;
            	Class<? extends Model> parentModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(getter));
				String tableName = LowerCaseStringCache.instance().get(Database.getTable(parentModelClass).getTableName());
            	sValue = parentDescription;
            	
            	_IPath parentTarget = modelAwareness.getPath().createRelativePath( modelAwareness.getPath().action() + 
            			( ObjectUtil.isVoid(modelAwareness.getPath().parameter()) ?  "" : "/" + modelAwareness.getPath().parameter() )+
            			"/" + tableName + "/show/" +  String.valueOf(parentId) );
            	if (parentTarget.canAccessControllerAction()){
                	control = new Link(parentTarget.getTarget());
                	control.setText(sValue);
            	}else {
            		control = new Label(sValue);
            	}
            }else {
                control = new Label(sValue);
            }
        }
        
        control.addClass(converter.getDisplayClassName());
        return control;
	}
	protected void _addFields(Row row, M record){
		int rowIndex = row.getParent().getContainedControls().size();
        for (String fieldName : getIncludedFields()) {
            Control control = getControl(getModelAwareness().getReflector().getModelClass().getSimpleName() + "["+rowIndex+"]." + fieldName,fieldName, record);

            Column column = null;
            if (getMetaprovider().isFieldVisible(fieldName)){
                column = row.createColumn(); 
            	Integer currentMaxFieldWidth = maxFieldWidth.get(fieldName);
            	Integer currentFieldWidth = Math.min(50,control.getText().length());
            	if (currentMaxFieldWidth == null || currentMaxFieldWidth < currentFieldWidth){
            		maxFieldWidth.put(fieldName,currentFieldWidth);
            	}
    		}else {
    			column = row.getLastColumn();
    		}
            column.addControl(control);

        }    	
		
	}
	protected void addRecordToTable(M record, BitSet showAction){
		Timer timer = startTimer("Adding one record to table",Config.instance().isTimerAdditive());
		try {
			_addRecordToTable(record, showAction);
		}finally {
			timer.stop();
		}
	}
    protected void _addRecordToTable(M record, BitSet showAction){
    	User u = (User)Database.getInstance().getCurrentUser();
    	Timer recordAccessibility = startTimer("Checking Record accessibility",Config.instance().isTimerAdditive());
    	try {
	    	if (u != null && !record.isAccessibleBy(u,modelAwareness.getReflector().getModelClass())){
	    		return;
	    	}
    	}finally {
    		recordAccessibility.stop();
    	}
    	Timer checkingIndexActionAccessibility = startTimer("Checking index action Accessibility",Config.instance().isTimerAdditive());
    	try {
	    	if (record.getId() > 0  && !modelAwareness.getPath().canAccessControllerAction("index",String.valueOf(record.getId()))){
	    		return;
	    	}
    	}finally {
    		checkingIndexActionAccessibility.stop();
    	}
        Row row = createRow();
        addLineLevelActions(row, record, showAction);
        addFields(row, record);
    }

}
