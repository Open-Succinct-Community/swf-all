package com.venky.swf.db.extensions;

import com.venky.extension.Extension;
import com.venky.swf.db.model.Model;

public abstract class AfterModelSave<M extends Model> implements Extension{

	public void invoke(Object... context) {
		M model = (M)context[0];
		afterSave(model);
	}
	
	public abstract void afterSave(M model);
}
