package com.venky.swf.views.controls.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Form;
import com.venky.swf.views.controls.page.text.AutoCompleteText;
import com.venky.swf.views.controls.page.text.TextArea;
import com.venky.swf.views.model.FieldUIMetaProvider;

public class ModelListEditableTable<M extends Model> extends ModelListTable<M>{

	public ModelListEditableTable(Path path, ModelAwareness modelAwareNess, FieldUIMetaProvider uimetaprovider, Form submittedWithForm) {
		super(path,modelAwareNess, uimetaprovider);
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
        
		if (!(control instanceof AutoCompleteText)){
	        control.addClass(converter.getDisplayClassName());
		}
        control.setForm(submittedWithForm.getId());
        
        return control;
	}
	protected int length(String value){
		if (value == null){
			return 0;
		}
		int length = 0; 
		StringTokenizer tok  = new StringTokenizer(value,"\r\n",false);
		while (tok.hasMoreTokens()){
			String token = tok.nextToken();
			length = Math.max(length, token.length());
		}
		return length;
	}
	protected int getDataLength(Control control){
		int length = 0; 
		String value = null; 
		value = control.getValue(); 
		if (ObjectUtil.isVoid(value)){
			value = control.getText();
		}
		
		if (!ObjectUtil.isVoid(value)){
			length = length(value);
		}
		if (length == 0 ){
			if (control instanceof AutoCompleteText){
				@SuppressWarnings("unchecked")
				AutoCompleteText<? extends Model> act = (AutoCompleteText<? extends Model>)control;
				length = act.getMaxDataLength();
			}else if (control instanceof TextArea){
				length = 50;
			}
		}
		
		return length;
	}
}
