/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.digest.Encryptor;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.layout.Table.TBody;
import com.venky.swf.views.controls.page.layout.Table.THead;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;
import com.venky.swf.views.controls.page.text.StatusBar;
import com.venky.swf.views.controls.page.text.StatusBar.Type;
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
        super(path,modelClass,includeFields);
        this.record = record;
    }
    
    private String formAction = "save";
    
    
    

    public void setFormAction(String formAction) {
		this.formAction = formAction;
	}



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
    protected void createBody(Body body) {
    	Form form = new Form();
    	body.addControl(form);
        form.setAction(getPath().controllerPath(), getFormAction());
        form.setMethod(SubmitMethod.POST);
        
    	Table table = new Table();
        form.addControl(table);
        String errorMsg = StringUtil.valueOf(record.removeTxnProperty("ui.error.msg"));
        if (!ObjectUtil.isVoid(errorMsg)){
            Row statusRow = table.createRow() ;
            Column column = statusRow.createColumn(getNumColumnsPerRow());
            column.addControl(new StatusBar(Type.ERROR, errorMsg));
        }
        

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
            	if (isFieldEditable(fieldName)){
                    Row r = getRow(table,true);
                    r.createColumn().addControl(fieldLabel);
                	if (fieldData instanceof TextArea){
                        r.createColumn(getNumColumnsPerRow()-r.numColumns()).addControl(fieldData);
                	}else {
                		r.createColumn().addControl(fieldData);
                	}
                	r.getLastColumn().addClass("data");
                }else {
                    Row r = getRow(table,true);
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
                    r.createColumn().addControl(fieldLabel);
                    r.createColumn().addControl(fieldData);
                    r.getLastColumn().addClass("data");
                }
            }else {
                fieldData.setVisible(false);
                hiddenFields.add(fieldData);
            }
            
            if (fieldData instanceof FileTextBox){
            	rpadLastRow(table);
            	form.setProperty("enctype","multipart/form-data");
            	FileTextBox ftb = (FileTextBox)fieldData;
            	ftb.setStreamUrl(getPath().controllerPath()+"/view/"+record.getId());
                Row streamRow = table.createRow();
                Column streamColumn = streamRow.createColumn(getNumColumnsPerRow());
                streamColumn.addControl(ftb.getStreamLink());
            }
            
            
            if (fieldData.isEnabled()){
            	if (hashFieldValue.length() > 0){
            		hashFieldValue.append(",");
            	}
            	String fieldValue = fieldData.getValue();
	            if (fieldData instanceof CheckBox){
	            	fieldValue = StringUtil.valueOf(((CheckBox)fieldData).isChecked());
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
            Submit sbm = new Submit();
            sbm.setName("_SUBMIT_MORE");
            sbm.setValue("Next");
            c.addControl(sbm);

            c = buttonRow.createColumn(getNumFieldsPerRow());
        	sbm = new Submit();
	        sbm.setName("_SUBMIT_NO_MORE");
	        sbm.setValue("Done");
	        c.addControl(sbm);
	        return;
        }else {
            c = buttonRow.createColumn(getNumColumnsPerRow());
            Submit sbm = new Submit();
            sbm.setName("_SUBMIT_NO_MORE");
            c.addControl(sbm);
        }
        
    	List<Class<? extends Model>> childModels = new ArrayList<Class<? extends Model>>();
    	for (Method childGetter: getReflector().getChildGetters()){
        	if (!List.class.isAssignableFrom(childGetter.getReturnType()) || getReflector().isAnnotationPresent(childGetter, HIDDEN.class)){
        		continue;
        	}
        	Class<? extends Model> childModelClass = getReflector().getChildModelClass(childGetter);
        	if (!childModels.contains(childModelClass)){
            	childModels.add(childModelClass);
        	}
        }
        for (Class<? extends Model> childClass: childModels){
			Path childPath = new Path(getPath().getTarget()+"/"+Database.getTable(childClass).getTableName().toLowerCase() + "/index");
        	childPath.setRequest(getPath().getRequest());
        	childPath.setResponse(getPath().getResponse());
        	childPath.setSession(getPath().getSession());
        	if (childPath.canAccessControllerAction()){
            	DashboardView view =  (DashboardView)childPath.invoke();
            	view.createBody(body,false);
        	}
        }    
    }
    protected boolean isFieldEditable(String fieldName) {
        return reflector.isFieldEditable(fieldName);
    }
    
    protected Kind getFieldProtection(String fieldName){
    	return reflector.getFieldProtection(fieldName);
    }
}
