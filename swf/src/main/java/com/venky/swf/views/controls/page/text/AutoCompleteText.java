/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import java.io.Reader;
import java.lang.reflect.Method;
import java.util.List;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Counts;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.views.controls._IControl;
import com.venky.swf.sql.Select;
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
	private ModelReflector<M> ref;
    private String descriptionField = null ;
    private TextBox hiddenField = null; 
    public AutoCompleteText(Class<M> modelClass){
    	this(modelClass,"");
    }
    public TextBox getHiddenField(){
    	return hiddenField;
    }
    
    private Integer maxDataLength = null;
    public int getMaxDataLength(){
    	if (maxDataLength == null){ 
	    	List<Counts> counts  = new Select("MAX(LENGTH("+descriptionField + ")) AS COUNT").from(modelClass).execute(Counts.class);
	    	if (counts.isEmpty()){
	    		maxDataLength = ref.getColumnDescriptor(descriptionField).getSize(); 
	    	}else {
	    		maxDataLength = counts.get(0).getCount();
	    	}
    	}
    	return maxDataLength;
    }
    
    public AutoCompleteText(Class<M> modelClass,String url){
        this.modelClass = modelClass;
        this.ref = ModelReflector.instance(modelClass);
        this.descriptionField = ref.getDescriptionField();
    	this.hiddenField = new TextBox();
    	this.hiddenField.setVisible(false);
    	if (Reader.class.isAssignableFrom(ref.getFieldGetter(descriptionField).getReturnType())){
        	addClass("reader");
        }
        setAutocompleteServiceURL(url);
        setVisible(true);
        setEnabled(true);
    	setWaterMark("Enter space to see complete list");
    	setToolTip("Enter the first few characters or space to see the full list.");
    }
    
    
    public Class<M> getModelClass(){
        return modelClass;
    }
    @Override
    public void setParent(_IControl parent){
        super.setParent(parent);
        parent.addControl(hiddenField);
    }    
    
    @Override
    public void setName(String name){
        int indexOfDot = name.indexOf('.');
        String autoCompleteFieldName = "_AUTO_COMPLETE_" + name;
        if (indexOfDot > 0){
        	autoCompleteFieldName = name.substring(0,indexOfDot) + "._AUTO_COMPLETE_" + name.substring(indexOfDot+1);
        }
        hiddenField.setName(name);
        super.setName(autoCompleteFieldName);
    }
    @Override
    public void setReadOnly(final boolean readonly){
    	super.setReadOnly(readonly);
    	if (hiddenField != null){
    		hiddenField.setReadOnly(readonly);
    	}
    }
    
    public void setEnabled(final boolean enabled){
    	super.setEnabled(enabled);
    	if (hiddenField != null){
    		hiddenField.setEnabled(enabled);
    	}
    }
    
    @Override
    public void setValue(Object value){
    	if (hiddenField != null){
    		hiddenField.setValue(value);
    	}
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
                super.setValue(dvalue);
            }
        }
    }
    
    @Override
    public void setForm(String formId){
    	super.setForm(formId);
    	if (hiddenField != null){
    		hiddenField.setForm(formId);
    	}
    }
    
}
