package com.venky.swf.db.annotations.column.ui;

import com.venky.swf.db.model.Model;

public interface OnLookupSelectionProcessor<M extends Model> {
	public void process(String fieldSelected,M partiallyFilledModel);
}
