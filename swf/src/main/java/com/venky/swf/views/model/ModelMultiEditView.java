package com.venky.swf.views.model;

import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.page.Body;

public class ModelMultiEditView<M extends Model> extends AbstractModelView<M>{

	public ModelMultiEditView(Path path, Class<M> modelClass,
			String[] includedFields) {
		super(path, modelClass, includedFields);
	}

	@Override
	protected void createBody(Body b) {
		// TODO Auto-generated method stub
		
	}

}
