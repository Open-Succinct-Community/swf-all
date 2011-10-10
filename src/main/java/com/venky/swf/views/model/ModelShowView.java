/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.lang.reflect.Method;
import java.util.List;

import com.venky.swf.db.Database;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.Path;
import com.venky.swf.views.controls.page.Body;

/**
 *
 * @author venky
 */
public class ModelShowView<M extends Model> extends ModelEditView<M> {

    public ModelShowView(Path path, Class<M> modelClass, String[] includeFields, M record) {
        super(path, modelClass, includeFields, record);
    }

    @Override
    protected boolean isFieldEditable(String fieldName) {
        return false;
    }
    
    @Override
    protected String getFormAction(){
        return "back";
    }
    
    protected void createBody (Body b){
    	super.createBody(b);
        for (Method childGetter: getReflector().getChildGetters()){
        	Class childClass = getReflector().getChildModelClass(childGetter);
        	List<Model> children;
			try {
				children = (List<Model>)childGetter.invoke(getRecord());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        	
        	new ModelListView<Model>(new Path(getPath().getTarget()+"/"+Database.getInstance().getTable(childClass).getTableName().toLowerCase()), 
        			childClass, null, children).createBody(b);
        }

    }
}
