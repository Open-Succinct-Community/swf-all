package com.venky.swf.plugins.lucene.views.model;

import java.util.List;

import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.controls.Control;
import com.venky.swf.views.controls.page.Body;
import com.venky.swf.views.model.ModelListView;

public class IndexedModelListView<M extends Model> extends ModelListView<M> {
	private Control searchControl = null;
	public IndexedModelListView(Path path, Class<M> modelClass,String[] includeFields, List<M> records, Control searchControl) {
		super(path, modelClass, includeFields, records);
		this.searchControl = searchControl;
	}
	
    @Override
	protected void createBody(Body b) {
		b.addControl(searchControl);
		super.createBody(b);
	}

}
