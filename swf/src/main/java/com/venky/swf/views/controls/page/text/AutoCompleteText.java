/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import java.io.Reader;
import java.lang.reflect.Method;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.layout.Div;
/**
 *
 * @author venky
 */
public class AutoCompleteText<M extends Model> extends Div {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<M> modelClass;
	private ModelReflector<M> ref;
    private String descriptionField = null ;
    private TextBox hiddenIdControl = null;
    private Control descriptionControl = null;
    
    public AutoCompleteText(Class<M> modelClass){
    	this(modelClass,"");
    }
    
    public AutoCompleteText(Class<M> modelClass,String url){
        this.modelClass = modelClass;
        this.ref = ModelReflector.instance(modelClass);
        this.descriptionField = ref.getDescriptionField();
    	this.hiddenIdControl = new TextBox();
    	this.hiddenIdControl.setVisible(false);
    	if (ref.isFieldValueALongText(descriptionField)){
    		this.descriptionControl = new TextArea(); 
    	}else {
    		this.descriptionControl = new TextBox();
    	}
    	
    	if (Reader.class.isAssignableFrom(ref.getFieldGetter(descriptionField).getReturnType())){
        	this.descriptionControl.addClass("reader");
        }
        
    	((_IAutoCompleteControl)descriptionControl).setAutocompleteServiceURL(url);
    	addControl(hiddenIdControl);
    	addControl(descriptionControl);
    	descriptionControl.setVisible(true);
        descriptionControl.setEnabled(true);
    	descriptionControl.setWaterMark("Enter * to see complete list");
    	descriptionControl.setToolTip("Enter the first few characters or * to see the full list.");
    }
    
    
    
    public Class<M> getModelClass(){
        return modelClass;
    }

    public TextBox getHiddenIdControl(){
    	return hiddenIdControl;
    }
    public Control getDescriptionControl(){
    	return descriptionControl;
    }
    @Override
    public void setName(String name){
        int indexOfDot = name.indexOf('.');
        String autoCompleteFieldName = "_AUTO_COMPLETE_" + name;
        if (indexOfDot > 0){
        	autoCompleteFieldName = name.substring(0,indexOfDot) + "._AUTO_COMPLETE_" + name.substring(indexOfDot+1);
        }
        if (hiddenIdControl != null){
        	hiddenIdControl.setName(name);
        }
        if (descriptionControl != null){
        	descriptionControl.setName(autoCompleteFieldName);
        }
    }
    @Override
    public String getName(){
    	if (descriptionControl != null){
    		return descriptionControl.getName();
    	}
    	return null;
    }
    @Override
    public void setReadOnly(final boolean readonly){
    	super.setReadOnly(readonly);
    	if (hiddenIdControl != null){
    		hiddenIdControl.setReadOnly(readonly);
    	}
    	if (descriptionControl != null){
    		descriptionControl.setReadOnly(readonly);
    	}
    }
    
    @Override
    public void setEnabled(final boolean enabled){
    	super.setEnabled(enabled);
    	if (hiddenIdControl != null){
    		hiddenIdControl.setEnabled(enabled);
    	}
    	if (descriptionControl != null){
    		descriptionControl.setEnabled(enabled);
    	}
    }
    
    @Override
    public void setValue(Object value){
    	if (hiddenIdControl != null){
    		hiddenIdControl.setValue(value);
    	}
    	if (descriptionControl != null){
	        if (!ObjectUtil.isVoid(value)){
				M model = Database.getTable(modelClass).get(Long.valueOf(String.valueOf(value)));
	            if (model != null) {
	                ModelReflector<M> reflector = ModelReflector.instance(modelClass);
	                Method descriptionGetter = reflector.getFieldGetter(descriptionField);
	                Object dvalue = null;
	                try {
	                    dvalue = descriptionGetter.invoke(model);
	                    dvalue = Database.getJdbcTypeHelper(reflector.getPool()).getTypeRef(descriptionGetter.getReturnType()).getTypeConverter().toString(dvalue);
	                } catch (Exception ex) {
	                    throw new RuntimeException(ex);
	                }
	                descriptionControl.setValue(dvalue);
	            }
	        }else {
	        	descriptionControl.setValue(null);
	        }
    	}
    }
    @Override
    public String getValue(){
    	if (descriptionControl != null){
    		return descriptionControl.getValue();
    	}
    	return null;
    }

    @Override
	public String getUnescapedValue(){
    	if (descriptionControl != null){
    		return descriptionControl.getUnescapedValue();
		}
    	return null;
	}


	@Override
    public void setForm(String formId){
    	if (hiddenIdControl != null){
    		hiddenIdControl.setForm(formId);
    	}
    	if (descriptionControl != null){
    		descriptionControl.setForm(formId);
    	}
    }
    
    public void setToolTip(String toolTip){
    	if (descriptionControl != null){
    		descriptionControl.setToolTip(toolTip);
    	}
    }
    
    public void setWaterMark(String watermark){
    	if (descriptionControl != null){
    		descriptionControl.setWaterMark(watermark);
    	}
    }
    
	public int getMaxDataLength() {
		return ref.getMaxDataLength(descriptionField);
	}
	public void setOnAutoCompleteSelectProcessingUrl(String url){
		if (descriptionControl != null){
			((_IAutoCompleteControl)descriptionControl).setOnAutoCompleteSelectProcessingUrl(url);
		}
	}
	
	
}
