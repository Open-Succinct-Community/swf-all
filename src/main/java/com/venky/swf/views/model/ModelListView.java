/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.layout.Table;
import com.venky.swf.views.controls.page.layout.Table.Column;
import com.venky.swf.views.controls.page.layout.Table.Row;
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
        Column action = header.createColumn(); 
        action.setText("V");
        action.setProperty("width", "1%");
        action = header.createColumn(); 
        action.setText("M");
        action.setProperty("width", "1%");
        action = header.createColumn(); 
        action.setText("D");
        action.setProperty("width", "1%");
        for (String fieldName : getIncludedFields()) {
            if (isFieldVisible(fieldName)) {
                header.createColumn().setText(getFieldLiteral(fieldName));
            }
        }

        for (M record : records) {
        	if (!record.isAccessibleBy(getPath().getSessionUser())){
        		continue;
        	}
            Row row = table.createRow();
            
            if (getPath().canAccessControllerAction("show")){
	            Link show = new Link();
	            show.setUrl(getPath().controllerPath()+"/show/"+record.getId());
	            show.addControl(new Image("/resources/images/show.png"));
	            row.createColumn().addControl(show);
            }else {
            	row.createColumn();
            }
            
            if (getPath().canAccessControllerAction("edit") && getPath().canAccessControllerAction("save")){
                Link edit = new Link();
                edit.setUrl(getPath().controllerPath()+"/edit/"+record.getId());
                edit.addControl(new Image("/resources/images/edit.png"));
                row.createColumn().addControl(edit);
            }else {
            	row.createColumn();
            }
            
            
            if (getPath().canAccessControllerAction("destroy")){
	            Link destroy = new Link();
	            destroy.setUrl(getPath().controllerPath()+"/destroy/"+record.getId());
	            destroy.addControl(new Image("/resources/images/destroy.png"));
	            row.createColumn().addControl(destroy);
            }else {
            	row.createColumn();
            }
            
            
            for (String fieldName : getIncludedFields()) {
                try {
                    if (isFieldVisible(fieldName)) {
                        Column column = row.createColumn(); 

                    	Method getter = getFieldGetter(fieldName);
                        Object value = getter.invoke(record);
                        TypeConverter<?> converter = Database.getInstance().getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
                        Control control = null;
                        
                        if (InputStream.class.isAssignableFrom(getter.getReturnType())){
                        	CONTENT_TYPE contentType = getter.getAnnotation(CONTENT_TYPE.class);
                        	if (contentType == null){
                        		contentType = new CONTENT_TYPE() {
									
									public Class<? extends Annotation> annotationType() {
										return CONTENT_TYPE.class;
									}
									
									public String value() {
										return "text/plain";
									}
								};
                        	}
                        	if (contentType.value().startsWith("image")){
                        		control = new Image(getPath().controllerPath()+"/view/"+record.getId());
                        	}else {
                        		control = new Link(getPath().controllerPath()+"/view/"+record.getId());
                        		control.setText("File");
                        	}
                        }else {
                            String sValue = converter.toString(converter.valueOf(value));
                            if (isFieldPassword(fieldName)){
                            	sValue = sValue.replaceAll(".", "\\*");
                            }
                            String parentDescription = getParentDescription(getter, record) ;
                            if (!ObjectUtil.isVoid(parentDescription)){
                            	Object parentId = getter.invoke(record);
                            	Class<? extends Model> parentModelClass = reflector.getReferredModelClass(reflector.getReferredModelGetterFor(getter));
                            	String tableName = Database.getInstance().getTable(parentModelClass).getTableName().toLowerCase();
                            	sValue = parentDescription;
                            	
                            	Path parentTarget = getPath().createRelativePath("/" + tableName + "/show/" +  String.valueOf(parentId));
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
                }
            }
        }
    }
}
