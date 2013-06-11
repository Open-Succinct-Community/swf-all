/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.venky.core.collections.LowerCaseStringCache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.string.StringUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.routing.Config;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.HotLink;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Div;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.layout.Table.TBody;
import com.venky.swf.views.controls.page.layout.Table.THead;
import com.venky.swf.views.controls.page.layout.Tabs;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.TextArea;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public class ModelEditView<M extends Model> extends AbstractModelView<M> {
    private int numFieldsPerRow = 2;
    private M record; 
    public ModelEditView(Path path,
                        Class<M> modelClass,
                        String[] includeFields,M record){
    	this(path,modelClass,includeFields,record,"save");
    }
    public ModelEditView(Path path,
            Class<M> modelClass,
            String[] includeFields,M record,String formAction){
		super(path,modelClass,includeFields);
		this.record = record;
		this.formAction = formAction;
	}
    private String formAction = null ;    
    

	public M getRecord() {
		return record;
	}

	public int getNumFieldsPerRow() {
        return numFieldsPerRow;
    }

    public void setNumFieldsPerRow(int numFieldsPerRow) {
        this.numFieldsPerRow = numFieldsPerRow;
    }

    public int getNumColumnsPerRow(){ 
        return 2 * getNumFieldsPerRow();
    }
    private Row getRow(Table table,boolean newRowIfFull){
        Row row = null;
        if (table.getContainedControls().isEmpty()){
            row = table.createRow();
        }else {
            List<_IControl> rows = table.getContainedControls();
            _IControl c = rows.get(rows.size()-1);
            if (c instanceof Row) {
            	row = (Row)c;
            }else if (c instanceof TBody || c instanceof THead){
            	rows = c.getContainedControls();
            	if (rows.isEmpty()){
            		if (c instanceof TBody) {
            			row = table.createRow();
            		}else {
            			row = table.createHeader();
            		}
            	}else {
            		row =  (Row)rows.get(rows.size() - 1);
            	}
        	}
        }
        if (row.numColumns() >= getNumColumnsPerRow() && newRowIfFull){
            row = table.createRow();
        }
        return row;
    }

    protected String getFormAction(){ 
    	if (getPath().canAccessControllerAction(formAction) || (getRecord().getRawRecord().isNewRecord() && getPath().canAccessControllerAction(formAction, null))){
    		return formAction;
    	}else {
    		return "back";
    	}
    }
    private Row rpadLastRow(Table table){
        Row r = getRow(table, false);
        if (r.numColumns() < getNumColumnsPerRow()){
        	r.createColumn(getNumColumnsPerRow() - r.numColumns());
        }
        return r;
    }
    
    @Override
    protected void createBody(_IControl body) {
    	Form form = new Form();
    	body.addControl(form);
    	String action = StringEscapeUtils.escapeHtml4(getPath().controllerPath());
    	Config.instance().getLogger(getClass().getName()).fine("action:" + action);
    	
        form.setAction(action, getFormAction());
        form.setMethod(SubmitMethod.POST);
        
    	Table table = new Table();
        form.addControl(table);
        

        Iterator<String> field = getIncludedFields().iterator();
        List<Control> hiddenFields = new ArrayList<Control>();
        
        TextBox hiddenHashField = new TextBox();
        hiddenHashField.setVisible(false);
        hiddenHashField.setName("_FORM_DIGEST");
        hiddenFields.add(hiddenHashField);
        
        StringBuilder hashFieldValue = new StringBuilder();
        
        while (field.hasNext()){
            String fieldName = field.next();
            
            Control fieldData = getInputControl(fieldName, record);
            Label fieldLabel = new Label(getFieldLiteral(fieldName));
            if (reflector.isFieldVisible(fieldName)){
            	if (fieldData instanceof TextArea || fieldData instanceof FileTextBox){
            		rpadLastRow(table);
            	}
            	
                Row r = getRow(table,true);
                r.createColumn().addControl(fieldLabel);
            	if (!isFieldEditable(fieldName)){
                    Kind protectionKind = getFieldProtection(fieldName);
                    switch(protectionKind){
                    	case NON_EDITABLE:
                    		fieldData.setEnabled(true);
                    		fieldData.setReadOnly(true);
                    		break;
                    	default:
                            fieldData.setEnabled(false);
                            break;
                    }
                }

            	if (fieldData instanceof TextArea){
                    r.createColumn(getNumColumnsPerRow()-r.numColumns()).addControl(fieldData);
            	}else {
            		r.createColumn().addControl(fieldData);
            	}
            	r.getLastColumn().addClass("data");
            }else {
                fieldData.setVisible(false);
                hiddenFields.add(fieldData);
            }
            
            if (fieldData instanceof FileTextBox){
            	rpadLastRow(table);
            	form.setProperty("enctype","multipart/form-data");
            	FileTextBox ftb = (FileTextBox)fieldData;
            	if (getReflector().getContentSize(record, fieldName) != 0 && !record.getRawRecord().isNewRecord()){
            		ftb.setStreamUrl(getPath().controllerPath()+"/view/"+record.getId(),getReflector().getContentName(record, fieldName));
                    Row streamRow = table.createRow();
                    Column streamColumn = streamRow.createColumn(getNumColumnsPerRow());
                    streamColumn.addControl(ftb.getStreamLink());
            	}
            }
            
            
            if (fieldData.isEnabled() && reflector.isFieldSettable(fieldName)){
            	if (hashFieldValue.length() > 0){
            		hashFieldValue.append(",");
            	}
            	String fieldValue = fieldData.getValue();
	            if (fieldData instanceof CheckBox){
	            	fieldValue = StringUtil.valueOf(((CheckBox)fieldData).isChecked());
	            }else if  (fieldData instanceof TextArea){
	            	fieldValue = fieldData.getText();
	            }
	            
            	hashFieldValue.append(fieldName);
            	hashFieldValue.append("=");
            	hashFieldValue.append(StringUtil.valueOf(fieldValue));
            }
        }
        
        hiddenHashField.setValue(Encryptor.encrypt(hashFieldValue.toString()));

        Row r = rpadLastRow(table);
        Column c = r.getLastColumn();
        for (Control hiddenField: hiddenFields){
            c.addControl(hiddenField);
        }
        
        Row buttonRow = table.createRow();
        
        if (getRecord().getRawRecord().isNewRecord()) {
            c = buttonRow.createColumn(getNumFieldsPerRow());
            Submit sbm = new Submit("Save & More");
            sbm.setToolTip("Done with this but more to go");
            sbm.setName("_SUBMIT_MORE");
            c.addControl(sbm);

            c = buttonRow.createColumn(getNumFieldsPerRow());
        	sbm = new Submit("Done");
        	sbm.setToolTip("Done with all");
	        sbm.setName("_SUBMIT_NO_MORE");
	        c.addControl(sbm);
	        return;
        }else {
            c = buttonRow.createColumn(getNumColumnsPerRow());
            String label = "Done";
            if (getFormAction().equals("back")){
            	label = "Close";
            }
        	Submit sbm = new Submit(label);
	        sbm.setName("_SUBMIT_NO_MORE");
	        c.addControl(sbm);
        }
        
    	List<Class<? extends Model>> childModels = getReflector().getChildModels(true, true);

    	
    	Tabs multiTab = null;
    	for (Class<? extends Model> childClass: childModels){
			Path childPath = new Path(getPath().getTarget()+"/"+ LowerCaseStringCache.instance().get(Database.getTable(childClass).getTableName()) + "/index");
        	childPath.setRequest(getPath().getRequest());
        	childPath.setResponse(getPath().getResponse());
        	childPath.setSession(getPath().getSession());
        	if (childPath.canAccessControllerAction()){
            	DashboardView view =  (DashboardView)childPath.invoke();
            	Div tab = new Div();
            	SequenceSet<HotLink> excludeLinks = new SequenceSet<HotLink>();
            	excludeLinks.addAll(getHotLinks());
            	
            	//Exclude back link on included child as it is a redundant link to the record in focus currently.
            	HotLink childBack = new HotLink(childPath.controllerPath() + "/back");
            	excludeLinks.add(childBack); 
            	
            	view.createBody(tab,false,excludeLinks);
        		if (multiTab == null){
        			multiTab = new Tabs();
        			body.addControl(multiTab);
        		}
            	multiTab.addSection(tab,childClass.getSimpleName());
        	}
        }    
    }
    protected boolean isFieldEditable(String fieldName) {
        return reflector.isFieldEditable(fieldName);
    }
    
    protected Kind getFieldProtection(String fieldName){
    	return reflector.getFieldProtection(fieldName);
    }
    
    private SequenceSet<HotLink> links = null;
    @Override
    public SequenceSet<HotLink> getHotLinks(){
    	if (links == null){
    		links = super.getHotLinks();
    		
            if (!getFormAction().equals("back")){
                HotLink sbm = new HotLink("#save");
                sbm.addControl(new Image("/resources/images/save.png","Submit"));
                sbm.setName("_SUBMIT_NO_MORE");
    	        links.add(sbm);
            }

	        if (getRecord().getRawRecord().isNewRecord()) {
                HotLink sbm = new HotLink("#save_and_move_to_next");
                sbm.addControl(new Image("/resources/images/next.png","Save & use as template for next."));
                sbm.setName("_SUBMIT_MORE");
                links.add(sbm);
            }
            

    		for (Method m : getSingleRecordActions()){
            	String actionName = m.getName();
            	if (actionName.equals("destroy") || actionName.equals(getPath().action())){
            		continue;
            	}
            	Link actionLink = createSingleRecordActionLink(m, getRecord());
            	if (actionLink != null) {
            		links.add(new HotLink(actionLink));
            	}
            }


    	}
    	return links;
    }
}
