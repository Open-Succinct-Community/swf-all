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
    }
    
    public void setVisible(boolean visible){
    	super.setVisible(false);
    	if (description != null){
    		description.setVisible(visible);
    	}
    }
    public Class<M> getModelClass(){
        return modelClass;
    }
    @Override
    public void setParent(_IControl parent){
        super.setParent(parent);
        description.setEnabled(isEnabled());
        parent.addControl(description);
    }    
    
    @Override
    public void setName(String name){
        super.setName(name);
        description.setName("_AUTO_COMPLETE_"+getName());
    }
    @Override
    public void setReadOnly(final boolean readonly){
    	super.setReadOnly(readonly);
    	description.setReadOnly(readonly);
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
}
