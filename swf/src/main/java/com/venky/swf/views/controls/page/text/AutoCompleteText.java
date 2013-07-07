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
import com.venky.swf.views.controls._IControl;

/**
 *
 * @author venky
 */
public class AutoCompleteText<M extends Model> extends TextBox{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Class<M> modelClass; 
    private String descriptionField = null ;
    private TextBox description = null; 
    public AutoCompleteText(Class<M> modelClass){
    	this(modelClass,"");
    }
    
    public void setOnAutoCompleteSelectProcessingUrl(String url){
    	description.setOnAutoCompleteSelectProcessingUrl(url);
    }
    public AutoCompleteText(Class<M> modelClass,String url){
        this.modelClass = modelClass;
        ModelReflector<M> ref = ModelReflector.instance(modelClass);
        this.descriptionField = ref.getDescriptionField();
    	this.description = new TextBox();
        if (Reader.class.isAssignableFrom(ref.getFieldGetter(descriptionField).getReturnType())){
        	this.description.addClass("reader");
        }
        this.description.setAutocompleteServiceURL(url);
        setVisible(true);
        setEnabled(true);
    	setWaterMark("Enter space to see complete list");
    	setToolTip("Enter the first few characters or space to see the full list.");
    }
    
    public void setVisible(boolean visible){
    	super.setVisible(false);
    	if (description != null){//Prevent NPE from super's constructor.
    		description.setVisible(visible);
    	}
    }
    
    
    public Class<M> getModelClass(){
        return modelClass;
    }
    @Override
    public void setParent(_IControl parent){
        super.setParent(parent);
        // SHOULD NOT BE NEEDED. description.setEnabled(isEnabled());
        parent.addControl(description);
    }    
    
    public void setWaterMark(String watermark){
    	if (description != null){
    		description.setWaterMark(watermark);
    	}
    }
    public void setToolTip(String watermark){
    	if (description != null){
    		description.setToolTip(watermark);
    	}
    }

    @Override
    public void setName(String name){
        super.setName(name);
        int indexOfDot = name.indexOf('.');
        String autoCompleteFieldName = "_AUTO_COMPLETE_" + name;
        if (indexOfDot > 0){
        	autoCompleteFieldName = name.substring(0,indexOfDot) + "._AUTO_COMPLETE_" + name.substring(indexOfDot+1);
        }
        description.setName(autoCompleteFieldName);
    }
    @Override
    public void setReadOnly(final boolean readonly){
    	super.setReadOnly(readonly);
    	description.setReadOnly(readonly);
    }
    
    public void setEnabled(final boolean enabled){
    	super.setEnabled(enabled);
    	if (description != null){ //Prevent NPE from super's constructor.
    		description.setEnabled(enabled);
    	}
    }
    
    @Override
    public void setValue(Object value){
        super.setValue(value);
        if (!ObjectUtil.isVoid(value)){
			M model = Database.getTable(modelClass).get(Integer.valueOf(String.valueOf(value)));
            if (model != null) {
                ModelReflector<M> reflector = ModelReflector.instance(modelClass);
                Method descriptionGetter = reflector.getFieldGetter(descriptionField);
                Object dvalue = null;
                try {
                    dvalue = descriptionGetter.invoke(model);
                    dvalue = Database.getJdbcTypeHelper().getTypeRef(descriptionGetter.getReturnType()).getTypeConverter().toString(dvalue);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                description.setValue(dvalue);
            }
        }
    }
    
    @Override
    public void setForm(String formId){
    	super.setForm(formId);
    	if (description != null){
    		description.setForm(formId);
    	}
    }
    public TextBox getDescriptionField(){
    	return description;
    }
}
