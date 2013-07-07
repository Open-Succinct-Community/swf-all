/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.views.model;

import java.util.List;

import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.HtmlView;
import com.venky.swf.views.controls.model.ModelAwareness;

/**
 *
 * @author venky
 */
public abstract class AbstractModelView<M extends Model> extends HtmlView implements FieldUIMetaProvider {

	private ModelAwareness modelAwareness = null;
    public ModelAwareness getModelAwareness() {
		return modelAwareness;
	}

	public AbstractModelView(Path path, final String[] includedFields) {
        super(path);
        modelAwareness = new ModelAwareness(path,includedFields);
    }
    
    public List<String> getIncludedFields() {
        return modelAwareness.getIncludedFields();
    }

	public boolean isFieldVisible(String fieldName) {
		return modelAwareness.getReflector().isFieldVisible(fieldName);
	}

	public boolean isFieldEditable(String fieldName) {
		return modelAwareness.getReflector().isFieldEditable(fieldName);
	}

	public Kind getFieldProtection(String fieldName) {
		return modelAwareness.getReflector().getFieldProtection(fieldName);
	}
}
