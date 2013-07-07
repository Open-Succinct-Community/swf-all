package com.venky.swf.views.controls.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.model.FieldUIMetaProvider;

public class ModelListEditableTable<M extends Model> extends ModelListTable<M>{

	public ModelListEditableTable(Path path, String[] includeFields, FieldUIMetaProvider uimetaprovider, Form submittedWithForm) {
		super(path,includeFields, uimetaprovider);
		this.submittedWithForm = submittedWithForm;
	}
	private Form submittedWithForm = null; 
	private static final long serialVersionUID = 5242933868664884989L;
	protected List<Method> getSingleRecordActions(){ 
		return new ArrayList<Method>();
	}
	@Override
	protected Control getControl(String controlName, String fieldName, M record){
		ModelAwareness helper = getModelAwareness();
		ModelReflector<M> reflector = helper.getReflector();
    	Method getter = reflector.getFieldGetter(fieldName);
		TypeConverter<?> converter = Database.getJdbcTypeHelper().getTypeRef(getter.getReturnType()).getTypeConverter();
        
		Control control = helper.getInputControl(controlName, fieldName, record, getMetaprovider());
        
        control.addClass(converter.getDisplayClassName());
        control.setForm(submittedWithForm.getId());
        
        return control;
	}
}
