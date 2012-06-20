/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.StringTokenizer;

import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.annotations.Depends;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.path.Path;
import com.venky.swf.path._IPath;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.Label;

/**
 *
 * @author venky
 */
public class ModelListView<M extends Model> extends AbstractModelView<M> {

    private List<M> records;

    public ModelListView(Path path, Class<M> modelClass, String[] includeFields, List<M> records) {
        super(path, modelClass, includeFields);
        this.records = records;
    }

    @Override
    protected void createBody(Body b) {
    	
    	Table container = new Table();
    	container.addClass("hfill");
    	b.addControl(container);
    	Row header = container.createHeader();
    	Column newLink = header.createColumn();

    	if (getPath().canAccessControllerAction("blank") && getPath().canAccessControllerAction("save")){
        	Link create = new Link();
            create.setUrl(getPath().controllerPath()+"/blank");
            create.addControl(new Image("/resources/images/blank.png"));
        	newLink.addControl(create);
    	}
    	
    	newLink.addControl(new Label(getModelClass().getSimpleName()));
        
    	Row rowContainingTable = container.createRow();
    	Column columnContainingTable = rowContainingTable.createColumn();
    	
    	
        Table table = new Table();
        columnContainingTable.addControl(table);
        
        table.setProperty("class", "tablesorter");
        header = table.createHeader();
        Column action = null ;
        
        for (Method m : getSingleRecordActions()){
            action = header.createColumn();
            action.setText(m.getName().substring(0,1));
            action.setProperty("width", "1%");
        }
        
        for (String fieldName : getIncludedFields()) {
            if (reflector.isFieldVisible(fieldName)) {
                header.createColumn().setText(getFieldLiteral(fieldName));
            }
        }

        for (M record : records) {
        	if (!record.isAccessibleBy((User)getPath().getSessionUser(),getModelClass())){
        		continue;
        	}
            Row row = table.createRow();
        	Timer timer = Timer.startTimer("paintAllActions");
            for (Method m : getSingleRecordActions()){
            	String actionName = m.getName();
            	boolean canAccessAction = getPath().canAccessControllerAction(actionName,String.valueOf(record.getId()));
            	Depends depends = getControllerReflector().getAnnotation(m,Depends.class);
            	if (canAccessAction && depends != null ){
            		StringTokenizer tok = new StringTokenizer(depends.value(),",") ;
            		while (tok.hasMoreTokens() && canAccessAction){
            			canAccessAction = canAccessAction && getPath().canAccessControllerAction(tok.nextToken(),String.valueOf(record.getId()));
            		}
            	}
            	if (canAccessAction){
                	SingleRecordAction sra = getControllerReflector().getAnnotation(m,SingleRecordAction.class);
                	String icon = null ; 
                	if (sra != null) {
                		icon = sra.icon();
                	}
                	if (icon == null){
                		icon = "/resources/images/show.png"; // Default icon.
                	}
    	            Link actionLink = new Link();
    	            actionLink.setUrl(getPath().controllerPath()+"/" + actionName +  "/"+record.getId());
    	            actionLink.addControl(new Image(icon));
    	            row.createColumn().addControl(actionLink);
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
    }
}
