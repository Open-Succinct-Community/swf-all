/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import static com.venky.core.log.TimerStatistics.Timer.startTimer;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.collections.UpperCaseStringCache;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelListView<M extends Model> extends AbstractModelView<M> {

    private List<M> records;
    private boolean indexedModel;
	private Map<String,Integer> suggestedFieldWidth = null ;
	private Map<String,Integer> maxFieldWidth = new HashMap<String,Integer>() ;
	
	public ModelListView(Path path, Class<M> modelClass,  List<M> records, Map<String,Integer> suggestedFieldWidth) {
		this(path,modelClass,suggestedFieldWidth == null ? null : suggestedFieldWidth.keySet().toArray(new String[]{}), records);
		if (suggestedFieldWidth != null){
			this.suggestedFieldWidth = new HashMap<String,Integer>();
			this.suggestedFieldWidth.putAll(suggestedFieldWidth);
		}
	}
	
	public ModelListView(Path path, Class<M> modelClass, String[] includeFields, List<M> records) {
		super(path, modelClass, includeFields);
		this.records = records;
        this.indexedModel = !getReflector().getIndexedFieldGetters().isEmpty();
        if (includeFields == null){
        	Iterator<String> fi = getIncludedFields().iterator(); 
        	while (fi.hasNext()){
        		String field = fi.next();
        		if (getReflector().isHouseKeepingField(field) || !getReflector().isFieldVisible(field)) {
	        		fi.remove();
	        	}
	        }
        }
        
        this.orderBy = new OrderBy();
    }
    
    public static Control createSearchForm(_IPath path){
    	com.venky.swf.views.controls.page.layout.Table table = new com.venky.swf.views.controls.page.layout.Table();
    	table.addClass("search");
    	
		Row row = table.createRow();
		TextBox search = new TextBox();
		search.setName("q");
		search.setValue(path.getFormFields().get("q"));

		row.createColumn().addControl(search);

		row.createColumn().addControl(new Submit("Search"));
		
		Form searchForm = new Form();
		searchForm.setAction(StringEscapeUtils.escapeHtml4(path.controllerPath()),"search");
		searchForm.setMethod(SubmitMethod.GET);
		
		searchForm.addControl(table);
		return searchForm;
    }
    
    private class OrderBy {
    	int sortDirection(){
    		if (StringUtil.equals(ascOrDesc, "ASC")){
    			return 0;
    		}else {
    			return 1;
    		}
    	}
    	String field = null;
    	String ascOrDesc = "ASC";
    	public OrderBy(){
    		String orderBy = new StringTokenizer(reflector.getOrderBy(), ",").nextToken();
    		StringTokenizer tok = new StringTokenizer(orderBy);
    		this.field = tok.nextToken();
    		if (tok.hasMoreTokens()){
        		this.ascOrDesc = UpperCaseStringCache.instance().get(tok.nextToken());
    		}
    	}
    }
    
    private SequenceSet<HotLink> links = null;
    @Override
    public SequenceSet<HotLink>  getHotLinks(){
    	if (links == null ){
    		links = super.getHotLinks();
    		if (getPath().canAccessControllerAction("blank") && getPath().canAccessControllerAction("save")){
            	HotLink create = new HotLink();
                create.setUrl(getPath().controllerPath()+"/blank");
                create.addControl(new Image("/resources/images/blank.png","New"));
            	links.add(create);
        	}
        	if (getPath().canAccessControllerAction("importxls") && getPath().canAccessControllerAction("save")){
        		HotLink importxls = new HotLink();
        		importxls.setUrl(getPath().controllerPath()+"/importxls");
        		importxls.addControl(new Image("/resources/images/importxls.png","Import"));
    			links.add(importxls);
        	}
        	
        	if (getPath().canAccessControllerAction("exportxls")){
        		HotLink exportxls = new HotLink();
        		exportxls.setUrl(getPath().controllerPath()+"/exportxls");
        		exportxls.addControl(new Image("/resources/images/exportxls.png","Export"));
    			links.add(exportxls);
        	}
    	}
    	return links;
    }
    
    protected void addHeadings(Row headerRow){
    	List<String> indexedFields = getReflector().getIndexedFields();
    	
        for (String fieldName : getIncludedFields()) {
        	String literal = getFieldLiteral(fieldName);
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
    	Map<String,Integer> fieldWidthMap = suggestedFieldWidth; 
    	
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
	protected void _addLineLevelActions(Row row, M record, BitSet showAction) {
    	List<Method> singleRecordActions = getSingleRecordActions();
        for (int actionIndex = 0 ; actionIndex < singleRecordActions.size() ; actionIndex ++ ){
        	Method m = singleRecordActions.get(actionIndex);
        	Column actionLinkCell = row.createColumn();
        	Link singleRecordActionLink = createSingleRecordActionLink(m, record);
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
	protected void _addFields(Row row, M record){
        for (String fieldName : getIncludedFields()) {
            Timer timer = startTimer("paintField." + fieldName,Config.instance().isTimerAdditive());
            try {
                Column column = row.createColumn(); 

            	Method getter = getFieldGetter(fieldName);
				TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
                Control control = null;
                
                if (InputStream.class.isAssignableFrom(getter.getReturnType())){
                	FileTextBox ftb = (FileTextBox)getInputControl(fieldName, record);
                	String contentName = getReflector().getContentName(record, fieldName);
        			if (getReflector().getContentSize(record, fieldName) != 0){
        				ftb.setStreamUrl(getPath().controllerPath()+"/view/"+record.getId(),contentName);
                    	control = ftb.getStreamLink();
        			}else {
        				control = new Label("No Attachment");
        			}
                }else {
                    Object value = getter.invoke(record);
                    String sValue = converter.toString(value);
                    if (reflector.isFieldPassword(fieldName)){
                    	sValue = sValue.replaceAll(".", "\\*");
                    }
                    String parentDescription = getParentDescription(getter, record) ;
                    if (!ObjectUtil.isVoid(parentDescription)){
                    	Object parentId = getter.invoke(record);
                    	Class<? extends Model> parentModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(getter));
						String tableName = LowerCaseStringCache.instance().get(Database.getTable(parentModelClass).getTableName());
                    	sValue = parentDescription;
                    	
                    	_IPath parentTarget = getPath().createRelativePath( getPath().action() + 
                    			( ObjectUtil.isVoid(getPath().parameter()) ?  "" : "/" + getPath().parameter() )+
                    			"/" + tableName + "/show/" +  String.valueOf(parentId) );
                    	if (parentTarget.canAccessControllerAction()){
                        	control = new Link(parentTarget.getTarget());
                        	control.setText(sValue);
                    	}else {
                    		control = new Label(sValue);
                    	}
                    }else {
                        column.addClass(converter.getDisplayClassName());
                    	control = new Label(sValue);
                    }
                }
                column.addControl(control);
            	Integer currentMaxFieldWidth = maxFieldWidth.get(fieldName);
            	Integer currentFieldWidth = Math.min(50,control.getText().length());
            	if (currentMaxFieldWidth == null || currentMaxFieldWidth < currentFieldWidth){
            		maxFieldWidth.put(fieldName,currentFieldWidth);
            	}
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }finally {
            	timer.stop();
            }
        }    	
		
	}
	protected void addRecordToTable(M record, BitSet showAction, Table table){
		Timer timer = startTimer("Adding one record to table",Config.instance().isTimerAdditive());
		try {
			_addRecordToTable(record, showAction, table);
		}finally {
			timer.stop();
		}
	}
    protected void _addRecordToTable(M record, BitSet showAction, Table table){
    	User u = (User)getPath().getSessionUser();
    	Timer recordAccessibility = startTimer("Checking Record accessibility",Config.instance().isTimerAdditive());
    	try {
	    	if (u != null && !record.isAccessibleBy(u,getModelClass())){
	    		return;
	    	}
    	}finally {
    		recordAccessibility.stop();
    	}
    	Timer checkingIndexActionAccessibility = startTimer("Checking index action Accessibility",Config.instance().isTimerAdditive());
    	try {
	    	if (record.getId() > 0  && !getPath().canAccessControllerAction("index",String.valueOf(record.getId()))){
	    		return;
	    	}
    	}finally {
    		checkingIndexActionAccessibility.stop();
    	}
        Row row = table.createRow();
        addLineLevelActions(row, record, showAction);
        addFields(row, record);
    }
    private OrderBy orderBy = null;
    @Override
    protected void createBody(_IControl b) {
    	
    	Table container = new Table();
    	container.addClass("hfill");
    	b.addControl(container);
    	
    	Row header = container.createHeader();
    	Column headerColumn = header.createColumn(2);
    	headerColumn.addControl(new Label(getModelClass().getSimpleName()));
    	if (indexedModel){
    		Row searchFormRow = container.createRow();
    		Column searchFormCell = searchFormRow.createColumn();
    		searchFormCell.addControl(createSearchForm(getPath()));
    		searchFormRow.createColumn();
    	}
    	
    	
    	
    	Row rowContainingTable = container.createRow();
    	Column columnContainingTable = rowContainingTable.createColumn(2);
    	
    	
        Table table = new Table();
        columnContainingTable.addControl(table);
        table.setClass("tablesorter");
        
        header = table.createHeader();
        BitSet showAction = addHeadingsForLineLevelActions(header);
        
        addHeadings(header);
        
        Timer timeToAddAllRecords = startTimer("Time to add allRecords" , Config.instance().isTimerAdditive());
        for (M record : records) {
        	addRecordToTable(record,showAction,table);        
    	  }
        timeToAddAllRecords.stop();
        
        Timer removingActions = startTimer("Removing actions not needed from table",Config.instance().isTimerAdditive());
        int numActionsRemoved = 0 ;
        int numActions = getSingleRecordActions().size();
        for (int i = 0 ; i < numActions ; i ++ ){
        	if (!showAction.get(i)){
        		table.removeColumn(i-numActionsRemoved);
        		numActionsRemoved ++; // Once a column is removed the indexes are shifted left. 
        	}
        }
        removingActions.stop();

        
        numActions -= numActionsRemoved;
    	int orderByFieldIndex = getIncludedFields().indexOf(orderBy.field);
        if (orderByFieldIndex >= 0 ){
        	table.setProperty("sortby", numActions + orderByFieldIndex);
        	table.setProperty("order", orderBy.sortDirection());
        }
        
        setWidths(header,numActions);
    }

}
