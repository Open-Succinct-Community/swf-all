/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
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
    protected Kind getFieldProtection(String fieldName){
    	return Kind.DISABLED;
    }
    @Override
    protected String getFormAction(){
        return "back";
    }
    
    protected void createBody (Body b){
    	super.createBody(b);
    }
}
