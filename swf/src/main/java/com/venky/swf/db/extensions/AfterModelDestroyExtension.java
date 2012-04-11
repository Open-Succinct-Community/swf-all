package com.venky.swf.db.extensions;

import com.venky.extension.Extension;
import com.venky.swf.db.model.Model;

public abstract class AfterModelDestroyExtension<M extends Model> implements Extension{

	public void invoke(Object... context) {
		M model = (M)context[0];
		afterDestroy(model);
	}
	
	public abstract void afterDestroy(M model);
}
