/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.controls.page.text;

import java.lang.reflect.Method;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_COLUMN;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.Table;
import com.venky.swf.views.controls.Control;

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
    private String descriptionColumn = null ;
    private TextBox description = null; 
    public AutoCompleteText(Class<M> modelClass){
    	this(modelClass,"");
    }
    public AutoCompleteText(Class<M> modelClass,String urlPrefix){
        this.modelClass = modelClass;
        Table<M> table = Database.getInstance().getTable(modelClass);
        
        descriptionColumn = "NAME";

        HAS_DESCRIPTION_COLUMN hdc = modelClass.getAnnotation(HAS_DESCRIPTION_COLUMN.class);
        if (hdc != null){
	        descriptionColumn = hdc.value();
        }
        if (ModelReflector.instance(modelClass).getFields().contains(descriptionColumn)){
	        this.description = new TextBox();
            description.setAutocompleteServiceURL(urlPrefix+"/"+table.getTableName().toLowerCase()+"/autocomplete/" );
            setVisible(false);
        }
    }
    public Class<M> getModelClass(){
        return modelClass;
    }
    @Override
    protected void setParent(Control parent){
        super.setParent(parent);
        if (description != null){
            description.setEnabled(isEnabled());
        	parent.addControl(description);
        }
    }    
    
    @Override
    public void setName(String name){
        super.setName(name);
        if (description != null){
            description.setName("_AUTO_COMPLETE_"+getName());
        }
    }
    
    @Override
    public void setValue(Object value){
        super.setValue(value);
        if (description != null && !ObjectUtil.isVoid(value)){
            M model = Database.getInstance().getTable(modelClass).get(Integer.valueOf(String.valueOf(value)));
            if (model != null) {
                ModelReflector<M> reflector = ModelReflector.instance(modelClass);
                Method descriptionGetter = reflector.getFieldGetter(descriptionColumn);
                Object dvalue = null;
                try {
                    dvalue = descriptionGetter.invoke(model);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                description.setValue(dvalue);
            }
        }
    }
}
