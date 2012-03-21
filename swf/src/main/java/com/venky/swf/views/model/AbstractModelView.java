/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.reflection.Reflector.MethodMatcher;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.controller.reflection.ControllerReflector;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.ui.CONTENT_TYPE;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTED;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.text.AutoCompleteText;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.FileTextBox;
import com.venky.swf.views.controls.page.text.PasswordText;
import com.venky.swf.views.controls.page.text.Select;
import com.venky.swf.views.controls.page.text.TextBox;

/**
 *
 * @author venky
 */
public abstract class AbstractModelView<M extends Model> extends HtmlView {

    Class<M> modelClass;
    ModelReflector<M> reflector;
    
    List<String> includedFields = new IgnoreCaseList();

    public AbstractModelView(Path path, Class<M> modelClass, final String[] includedFields) {
        super(path);
        this.modelClass = modelClass;
        this.reflector = ModelReflector.instance(modelClass);

        this.includedFields.addAll(reflector.getFields());
        if (includedFields != null && includedFields.length > 0) {
            this.includedFields.retainAll(Arrays.asList(includedFields));
        }
        
        ControllerReflector<? extends Controller> ref = new ControllerReflector(path.controller().getClass(),Controller.class);
        singleRecordActions = ref.getMethods(new SingleRecordActionMatcher(ref));
    }
    
    private class SingleRecordActionMatcher implements MethodMatcher {
    	final ControllerReflector<? extends Controller> ref ;
    	public SingleRecordActionMatcher(ControllerReflector<? extends Controller> ref) {
    		this.ref = ref;
		}
		public boolean matches(Method method) {
			return ref.isAnnotationPresent(method,SingleRecordAction.class);
		} 
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
        TypeConverter<?> converter = Database.getInstance().getJdbcTypeHelper().getTypeRef(returnType).getTypeConverter();
        Control control = null;
        if (boolean.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType)) {
            CheckBox cb = new CheckBox();
            cb.setChecked(converter.toString(value));
            control = cb;
        }else if (InputStream.class.isAssignableFrom(returnType) || Reader.class.isAssignableFrom(returnType)){
        	FileTextBox ftb = new FileTextBox();
        	CONTENT_TYPE type = reflector.getAnnotation(getter,CONTENT_TYPE.class);
        	if (type != null){
        		ftb.setContentType(type.value());
        	}
        	control = ftb;
        }else {
            Method parentModelGetter = getReflector().getReferredModelGetterFor(getter);
            if (parentModelGetter != null){
                control = new AutoCompleteText(getReflector().getReferredModelClass(parentModelGetter),getPath().getBackTarget());
            }else if (isFieldPassword(fieldName)){
                control = new PasswordText();
            }else if (isFieldEnumeration(fieldName)){
                Select select = new Select();
                Enumeration enumeration = getReflector().getAnnotation(getFieldGetter(fieldName),Enumeration.class) ;
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
        control.setName(fieldName);
        return control;
    }

    protected boolean isFieldVisible(String fieldName) {
        return !isFieldHidden(fieldName);
    }
    protected boolean isFieldHidden(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return getReflector().isAnnotationPresent(getter,HIDDEN.class);
	}
    
    protected boolean isFieldPassword(String fieldName){
        Method getter = getFieldGetter(fieldName);
        return  getReflector().isAnnotationPresent(getter,PASSWORD.class);
    }
    
    protected boolean isFieldProtected(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return getReflector().isAnnotationPresent(getter,PROTECTED.class);
    }
    
    protected boolean isFieldVirtual(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return getReflector().isAnnotationPresent(getter,IS_VIRTUAL.class);
    }
    
    protected boolean isFieldEnumeration(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return getReflector().isAnnotationPresent(getter,Enumeration.class);
    }
    
    
    protected String getParentDescription(Method parentIdGetter, Model record) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method parentModelGetter = getReflector().getReferredModelGetterFor(parentIdGetter);
        
        if (parentModelGetter != null){
            Model parentModel = (Model)parentModelGetter.invoke(record);
            if (parentModel != null){
                @SuppressWarnings("unchecked")
				Class<Model> parentModelClass = (Class<Model>)parentModelGetter.getReturnType();
                ModelReflector<Model> parentModelReflector = ModelReflector.instance(parentModelClass);
                String descriptionColumn = parentModelReflector.getDescriptionColumn();
                Object descValue = parentModelReflector.getFieldGetter(descriptionColumn).invoke(parentModel);
                return StringUtil.valueOf(descValue);
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
}