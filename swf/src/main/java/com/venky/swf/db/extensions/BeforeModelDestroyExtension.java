package com.venky.swf.db.extensions;

import com.venky.extension.Extension;
import com.venky.swf.db.model.Model;

public abstract class BeforeModelDestroyExtension<M extends Model> implements Extension{

	@Override
	public void invoke(Object... context) {
		M model = (M)context[0];
		beforeDestroy(model);
	}
	
	public abstract void beforeDestroy(M model);
}
