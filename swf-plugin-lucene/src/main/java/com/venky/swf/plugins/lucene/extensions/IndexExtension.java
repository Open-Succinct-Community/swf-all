package com.venky.swf.plugins.lucene.extensions;

import java.lang.reflect.Proxy;

import com.venky.extension.Extension;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelInvocationHandler;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;

public abstract class IndexExtension implements Extension{
	protected Class<? extends Model> getRealModelClass(Model proxy){
		if (Proxy.isProxyClass(proxy.getClass())){
			ModelInvocationHandler handler = (ModelInvocationHandler)Proxy.getInvocationHandler(proxy);
			return handler.getReflector().getRealModelClass();
		}else {
			throw new RuntimeException("Model parameter is not a Dynamic proxy");
		}
	}
	public void invoke(Object... context) {
		Model proxy =  (Model)context[0];
		Class<? extends Model> realModelClass = getRealModelClass(proxy);
		if (realModelClass != null){
			LuceneIndexer indexer = LuceneIndexer.instance(ModelReflector.instance(realModelClass));
			updateIndex(indexer,proxy.getRawRecord());
		}
	}
	
	protected abstract void updateIndex(LuceneIndexer indexer, Record record);

}
