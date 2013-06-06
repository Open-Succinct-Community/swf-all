/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.ui.TOOLTIP;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Image;
import com.venky.swf.views.controls.page.Link;
import com.venky.swf.views.controls.page.text.AutoCompleteText;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.DateBox;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.Select;
import com.venky.swf.views.controls.page.text.TextArea;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public abstract class AbstractModelView<M extends Model> extends HtmlView {

    Class<M> modelClass;
    ModelReflector<M> reflector;
    ControllerReflector<? extends Controller> controllerReflector;
    
	List<String> includedFields = new IgnoreCaseList(false);

    public AbstractModelView(Path path, Class<M> modelClass, final String[] includedFields) {
        super(path);
        this.modelClass = modelClass;
        this.reflector = ModelReflector.instance(modelClass);

    	this.includedFields.addAll(reflector.getFields());
        if (includedFields != null && includedFields.length > 0){
        	this.includedFields.retainAll(Arrays.asList(includedFields));
        }
        
        controllerReflector = ControllerReflector.instance(path.getControllerClass());
        singleRecordActions = controllerReflector.getSingleRecordActionMethods();
    }
    
    public ControllerReflector<? extends Controller> getControllerReflector() {
		return controllerReflector;
	}
    
    public ModelReflector<M> getReflector() {
        return reflector;
    }
    
    public List<String> getIncludedFields() {
        return includedFields;
    }

    public Class<M> getModelClass() {
        return modelClass;
    }
    private Map<String, Method> getterMap = new IgnoreCaseMap<Method>();

    public Method getFieldGetter(String fieldName) {
        Method getter = getterMap.get(fieldName);
        if (getter == null) {
            getter = reflector.getFieldGetter(fieldName);
            getterMap.put(fieldName, getter);
        }
        return getter;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Control getInputControl(String fieldName, M record) {
        Method getter = getFieldGetter(fieldName);
        Object value = null;
        try {
            value = getter.invoke(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Class<?> returnType = getter.getReturnType();
        TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(returnType).getTypeConverter();
        Control control = null;
        if (boolean.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType)) {
            CheckBox cb = new CheckBox();
            cb.setChecked(converter.toString(value));
            control = cb;
        }else if (Reader.class.isAssignableFrom(returnType)){
        	TextArea txtArea = new TextArea();
        	txtArea.setText(converter.toString(value));
        	control = txtArea;
        }else if (InputStream.class.isAssignableFrom(returnType)){
        	FileTextBox ftb = new FileTextBox();
        	String contentType = reflector.getContentType(record, fieldName);
			if (!ObjectUtil.isVoid(contentType)){
				ftb.setContentType(contentType);
			}
        	control = ftb;
        }else {
            Method parentModelGetter = getReflector().getReferredModelGetterFor(getter);
            if (parentModelGetter != null){
                control = new AutoCompleteText(getReflector().getReferredModelClass(parentModelGetter),getPath().controllerPath()+"/autocomplete");
            }else if (Date.class.isAssignableFrom(returnType)){
            	control = new DateBox(); 
            }else if (reflector.isFieldPassword(fieldName)){
                control = new PasswordText();
            }else if (reflector.isFieldEnumeration(fieldName)){
                Select select = new Select();
                Enumeration enumeration = reflector.getAnnotation(getFieldGetter(fieldName),Enumeration.class) ;
                StringTokenizer allowedValues = new StringTokenizer(enumeration.value(),",");
                
                while (allowedValues.hasMoreTokens()){
                	String nextAllowedValue = allowedValues.nextToken();
                	select.createOption(nextAllowedValue, nextAllowedValue);
                }
                control = select;
            }else {
            	control = new TextBox();
            }
            if (!ObjectUtil.isVoid(value)){
            	control.setValue(converter.toString(value));
            }
        }
        WATERMARK watermark = getReflector().getAnnotation(getter, WATERMARK.class);
        if (watermark != null){
        	control.setWaterMark(watermark.value());
        }
        TOOLTIP tooltip = getReflector().getAnnotation(getter, TOOLTIP.class);
        if (tooltip != null){
        	control.setToolTip(tooltip.value());
        }
        
        control.setName(fieldName);
        return control;
    }

    protected String getParentDescription(Method parentIdGetter, Model record) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method parentModelGetter = getReflector().getReferredModelGetterFor(parentIdGetter);
        
        if (parentModelGetter != null){
            Model parentModel = (Model)parentModelGetter.invoke(record);
            if (parentModel != null){
                @SuppressWarnings("unchecked")
				Class<? extends Model> parentModelClass = (Class<? extends Model>)parentModelGetter.getReturnType();
                ModelReflector<? extends Model> parentModelReflector = ModelReflector.instance(parentModelClass);
                String descriptionColumn = parentModelReflector.getDescriptionField();
                Method descGetter = parentModelReflector.getFieldGetter(descriptionColumn);
                Object descValue = descGetter.invoke(parentModel);
                return Database.getJdbcTypeHelper().getTypeRef(descGetter.getReturnType()).getTypeConverter().toString(descValue);
            }
        }
        return null;
    }

    protected String getFieldLiteral(String fieldName){
        String fieldLiteral =  StringUtil.camelize(fieldName);

        Method parentModelgetter = getReflector().getReferredModelGetterFor(reflector.getFieldGetter(fieldName));
        if (parentModelgetter != null) {
            fieldLiteral = parentModelgetter.getName().substring("get".length()) ;
        }
        return fieldLiteral;
        
    }

    List<Method> singleRecordActions = new ArrayList<Method>();
    public List<Method> getSingleRecordActions(){
    	return singleRecordActions;
    }
    
    public Link createSingleRecordActionLink(Method m, M record){
    	String actionName = m.getName();
    	SingleRecordAction sra = getControllerReflector().getAnnotation(m,SingleRecordAction.class);
    	
    	if (sra == null){
    		return null;
    	}
    	
    	boolean canAccessAction = record.getId() > 0  && getPath().canAccessControllerAction(actionName,String.valueOf(record.getId()));
    	if (!canAccessAction){
    		return null;
    	}
    	Link actionLink = new Link();
    	String icon = "/resources/images/show.png" ; 
    	String tooltip = StringUtil.camelize(actionName);
    	if (sra != null) {
    		if (!ObjectUtil.isVoid(sra.icon())){
        		icon = sra.icon(); 
    		}
    		if (!ObjectUtil.isVoid(sra.tooltip())){
        		tooltip = sra.tooltip(); 
    		}
    	}
        StringBuilder sAction = new StringBuilder();
        if ("search".equals(getPath().action())){
        	sAction.append(getPath().controllerPath()).append("/").append(getPath().action()).append("/").append(getPath().getFormFields().get("q"));
        }else {
        	sAction.append(getPath().getTarget());
        }
    	sAction.append("/").append(getPath().controllerPathElement());
    	sAction.append("/").append(actionName).append("/").append(record.getId());
    	actionLink.setUrl(sAction.toString());

    	actionLink.addControl(new Image(icon,tooltip));

    	return actionLink;
    }
}
