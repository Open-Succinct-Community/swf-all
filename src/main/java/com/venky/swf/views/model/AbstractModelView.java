/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.venky.core.collections.IgnoreCaseList;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.annotations.column.PASSWORD;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.page.text.AutoCompleteText;
import com.venky.swf.views.controls.page.text.CheckBox;
import com.venky.swf.views.controls.page.text.Input;
import com.venky.swf.views.controls.page.text.PasswordText;
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

    public Input getInputControl(String fieldName, M record) {
        Method getter = getFieldGetter(fieldName);
        Object value = null;
        try {
            value = getter.invoke(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Class<?> returnType = getter.getReturnType();
        TypeConverter<?> converter = Database.getInstance().getJdbcTypeHelper().getTypeRef(returnType).getTypeConverter();
        Input control = null;
        if (boolean.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType)) {
            CheckBox cb = new CheckBox();
            cb.setChecked(converter.toString(value));
            control = cb;
        } else {
            Method parentModelGetter = ModelReflector.getParentModelGetterFor(getter);
            if (parentModelGetter != null){
                control = new AutoCompleteText(ModelReflector.getParentModelClass(parentModelGetter));
            }else if (isFieldPassword(fieldName)){
                control = new PasswordText();
            }else {
                control = new TextBox();
            }
            control.setValue(converter.toString(value));
        }
        control.setName(fieldName);
        return control;
    }

    protected boolean isFieldVisible(String fieldName) {
        return !isFieldHidden(fieldName);
    }
    protected boolean isFieldHidden(String fieldName){
    	Method getter = getFieldGetter(fieldName);
    	return getter.isAnnotationPresent(HIDDEN.class);
	}
    
    protected boolean isFieldPassword(String fieldName){
        Method getter = getFieldGetter(fieldName);
        return  getter.isAnnotationPresent(PASSWORD.class);

    }
    protected String getParentDescription(Method parentIdGetter, Model record) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method parentModelGetter = ModelReflector.getParentModelGetterFor(parentIdGetter);
        
        if (parentModelGetter != null){
            Model parentModel = (Model)parentModelGetter.invoke(record);
            if (parentModel != null){
                Class<Model> parentModelClass = (Class<Model>)parentModelGetter.getReturnType(); 
                HAS_DESCRIPTION_COLUMN hdc = parentModelClass.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
                if (hdc != null){
                    String descriptionColumn = hdc.value();
                    assert (ModelReflector.instance(parentModelClass).getFields().contains(descriptionColumn));
    
                    Object descValue = ModelReflector.instance(parentModelClass).getFieldGetter(descriptionColumn).invoke(parentModel);
                    return StringUtil.valueOf(descValue);
                }
            }
        }
        return null;
    }

    protected String getFieldLiteral(String fieldName){
        String fieldLiteral =  StringUtil.camelize(fieldName);

        Method parentModelgetter = ModelReflector.getParentModelGetterFor(reflector.getFieldGetter(fieldName));
        if (parentModelgetter != null) {
            Class<?> parentModel = parentModelgetter.getReturnType();
            HAS_DESCRIPTION_COLUMN hdc = parentModel.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
            if (hdc != null){
                fieldLiteral = parentModelgetter.getName().substring("get".length()) ;
            }
        }
        
        return fieldLiteral;
        
    }

}
