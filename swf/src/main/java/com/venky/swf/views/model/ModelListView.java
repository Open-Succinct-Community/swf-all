/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.Form.SubmitMethod;
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
    }
    
    public static Control createSearchForm(_IPath path){
    	com.venky.swf.views.controls.page.layout.Table table = new com.venky.swf.views.controls.page.layout.Table();
		Row row = table.createRow();
		TextBox search = new TextBox();
		search.setName("q");
		search.setValue(path.getFormFields().get("q"));

		row.createColumn().addControl(new Label("Search"));
		row.createColumn().addControl(search);

		row.createColumn().addControl(new Submit("Search"));
		
		Form searchForm = new Form();
		searchForm.setAction(path.controllerPath(),"search");
		searchForm.setMethod(SubmitMethod.GET);
		
		searchForm.addControl(table);
		return searchForm;
    }

    @Override
    protected void createBody(Body b) {
    	if (indexedModel){
    		b.addControl(createSearchForm(getPath()));
    	}
    	
    	Table container = new Table();
    	container.addClass("hfill");
    	b.addControl(container);
    	Row header = container.createHeader();
    	Column newLink = header.createColumn();

    	if (getPath().canAccessControllerAction("blank") && getPath().canAccessControllerAction("save")){
        	Link create = new Link();
            create.setUrl(getPath().controllerPath()+"/blank");
            create.addControl(new Image("/resources/images/blank.png","New"));
        	newLink.addControl(create);
    	}
    	if (getPath().canAccessControllerAction("importxls") && getPath().canAccessControllerAction("save")){
    		Link importxls = new Link();
    		importxls.setUrl(getPath().controllerPath()+"/importxls");
    		importxls.addControl(new Image("/resources/images/importxls.png","Import"));
    		newLink.addControl(importxls);
    	}
    	if (getPath().canAccessControllerAction("exportxls")){
    		Link exportxls = new Link();
    		exportxls.setUrl(getPath().controllerPath()+"/exportxls");
    		exportxls.addControl(new Image("/resources/images/exportxls.png","Export"));
    		newLink.addControl(exportxls);
    	}
    	if (getPath().canAccessControllerAction("truncate") && getPath().canAccessControllerAction("destroy")){
    		Link truncate = new Link();
    		truncate.setUrl(getPath().controllerPath()+"/truncate");
    		truncate.addControl(new Image("/resources/images/destroy.png","Truncate"));
    		newLink.addControl(truncate);
    	}
    	newLink.addControl(new Label(getModelClass().getSimpleName()));
        
    	Row rowContainingTable = container.createRow();
    	Column columnContainingTable = rowContainingTable.createColumn();
    	
    	
        Table table = new Table();
        columnContainingTable.addControl(table);
        
        table.setProperty("class", "tablesorter");
        header = table.createHeader();
        Column action = null ;
        
        BitSet showAction = new BitSet();
        int numActions = getSingleRecordActions().size(); 
        
        for (int i = 0 ; i < numActions ; i ++ ){
        	action = header.createColumn();
            //action.setText(StringUtil.camelize(m.getName()));
            action.setProperty("width", "1%");
            showAction.clear(i);
        }
        
        boolean hasAtleastOneVisbleColumn = false; 
        for (String fieldName : getIncludedFields()) {
            if (reflector.isFieldVisible(fieldName)) {
                header.createColumn().setText(getFieldLiteral(fieldName));
                hasAtleastOneVisbleColumn = true;
            }
        }

        for (M record : records) {
        	if (!record.isAccessibleBy((User)getPath().getSessionUser(),getModelClass())){
        		continue;
        	}
        	if (record.getId() > 0  && !getPath().canAccessControllerAction("index",String.valueOf(record.getId()))){
        		continue;
        	}
            Row row = table.createRow();
        	Timer timer = Timer.startTimer("paintAllActions");
        	List<Method> singleRecordActions = getSingleRecordActions();
            for (int actionIndex = 0 ; actionIndex < singleRecordActions.size() ; actionIndex ++ ){
            	Method m = singleRecordActions.get(actionIndex);
            	String actionName = m.getName();
            	boolean canAccessAction = record.getId() > 0  && getPath().canAccessControllerAction(actionName,String.valueOf(record.getId()));
            	if (canAccessAction){
                	SingleRecordAction sra = getControllerReflector().getAnnotation(m,SingleRecordAction.class);
                	String icon = "/resources/images/show.png" ; 
                	String tooltip = actionName;
                	if (sra != null) {
                		if (!ObjectUtil.isVoid(sra.icon())){
                    		icon = sra.icon(); 
                		}
                		if (!ObjectUtil.isVoid(sra.tooltip())){
                    		tooltip = sra.tooltip(); 
                		}
                	}
    	            Link actionLink = new Link();
    	            StringBuilder sAction = new StringBuilder();
    	            if ("search".equals(getPath().action())){
    	            	sAction.append(getPath().controllerPath()).append("/").append(getPath().action()).append("/").append(getPath().getFormFields().get("q"));
    	            }
	            	sAction.append(getPath().controllerPath()).append("/").append(actionName).append("/").append(record.getId());
	            	actionLink.setUrl(sAction.toString());

	            	actionLink.addControl(new Image(icon,tooltip));
    	            row.createColumn().addControl(actionLink);
    	            showAction.set(actionIndex);
            	}else{
                	row.createColumn();
                }
            }
            timer.stop();
            for (String fieldName : getIncludedFields()) {
                timer = Timer.startTimer("paintField." + fieldName);
                try {
                    if (reflector.isFieldVisible(fieldName)) {
                        Column column = row.createColumn(); 

                    	Method getter = getFieldGetter(fieldName);
						TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
                        Control control = null;
                        
                        if (InputStream.class.isAssignableFrom(getter.getReturnType())){
                        	FileTextBox ftb = (FileTextBox)getInputControl(fieldName, record);
                        	ftb.setStreamUrl(getPath().controllerPath()+"/view/"+record.getId());
                        	control = ftb.getStreamLink();
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
								String tableName = Database.getTable(parentModelClass).getTableName().toLowerCase();
                            	sValue = parentDescription;
                            	
                            	_IPath parentTarget = getPath().createRelativePath("/show/" + String.valueOf(record.getId()) + "/" + tableName + "/show/" +  String.valueOf(parentId));
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
        
        int numActionsRemoved = 0 ;
        
        for (int i = 0 ; i < numActions ; i ++ ){
        	if (!showAction.get(i)){
        		table.removeColumn(i-numActionsRemoved);
        		numActionsRemoved ++; // Once a column is removed the indexes are shifted left. 
        	}
        }

        numActions -= numActionsRemoved;
        if (hasAtleastOneVisbleColumn){
        	table.setProperty("sortby", numActions);
        }
    }
}
