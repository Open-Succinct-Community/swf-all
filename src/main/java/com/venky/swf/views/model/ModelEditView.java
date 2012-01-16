/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.Path;
import com.venky.swf.views.DashboardView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
import com.venky.swf.views.controls.page.buttons.Submit;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.layout.Table.TBody;
import com.venky.swf.views.controls.page.layout.Table.THead;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;

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
            List<Control> rows = table.getContainedControls();
            Control c = rows.get(rows.size()-1);
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
    	if (getPath().canAccessControllerAction("save") || (getRecord().getRawRecord().isNewRecord() && getPath().canAccessControllerAction("save", null))){
    		return "save";
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
        Iterator<String> field = getIncludedFields().iterator();
        List<Control> hiddenFields = new ArrayList<Control>();
        while (field.hasNext()){
            String fieldName = field.next();
            Control fieldData = getInputControl(fieldName, record);
            Label fieldLabel = new Label(getFieldLiteral(fieldName));
        	if (isFieldEditable(fieldName)){
                Row r = getRow(table,true);
                r.createColumn().addControl(fieldLabel);
                r.createColumn().addControl(fieldData);
            }else if (isFieldVisible(fieldName)){
                Row r = getRow(table,true);
                fieldData.setEnabled(false);
                r.createColumn().addControl(fieldLabel);
                r.createColumn().addControl(fieldData);
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
        }
        Row r = rpadLastRow(table);
        Column c = r.getLastColumn();
        for (Control hiddenField: hiddenFields){
            c.addControl(hiddenField);
        }
        Row buttonRow = table.createRow();
        c = buttonRow.createColumn(getNumColumnsPerRow());
        Submit sbm = new Submit();
        c.addControl(sbm);
        
        if (getRecord().getRawRecord().isNewRecord()) {
        	return;
        }
    	List<Class<? extends Model>> childModels = new ArrayList<Class<? extends Model>>();
    	for (Method childGetter: getReflector().getChildGetters()){
        	if (!List.class.isAssignableFrom(childGetter.getReturnType())){
        		continue;
        	}
        	Class<? extends Model> childModelClass = getReflector().getChildModelClass(childGetter);
        	if (!childModels.contains(childModelClass)){
            	childModels.add(childModelClass);
        	}
        }
        for (Class<? extends Model> childClass: childModels){
        	Path childPath = new Path(getPath().getTarget()+"/"+Database.getInstance().getTable(childClass).getTableName().toLowerCase() + "/index");
        	childPath.setRequest(getPath().getRequest());
        	childPath.setResponse(getPath().getResponse());
        	childPath.setSession(getPath().getSession());
        	if (childPath.canAccessControllerAction()){
            	DashboardView view = (DashboardView)childPath.invoke();
            	view.createBody(body,false);
        	}
        }


    }
    
    
    protected boolean isFieldEditable(String fieldName){
        return isFieldVisible(fieldName) && !isFieldVirtual(fieldName) && !isFieldProtected(fieldName);
    }
}
