package com.venky.swf.plugins.background.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;

public class AfterCommitExtension implements Extension{
	private static AfterCommitExtension instance = new AfterCommitExtension();
	static {
		Registry.instance().registerExtension("after.commit", instance);
	}
	public void invoke(Object... context) {
		Transaction txn = (Transaction)context[0];
		if (!txn.getCache(ModelReflector.instance(DelayedTask.class)).isEmpty()){
			TaskManager.instance().wakeUp();
		}
	}
	
}
