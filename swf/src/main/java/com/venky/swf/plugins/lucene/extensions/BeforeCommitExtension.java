package com.venky.swf.plugins.lucene.extensions;

import java.util.List;

import org.apache.lucene.document.Document;

import com.venky.cache.Cache;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db._IDatabase._ITransaction;
import com.venky.swf.plugins.lucene.index.background.IndexManager;

public class BeforeCommitExtension implements Extension{
	private static BeforeCommitExtension instance = new BeforeCommitExtension();
	static {
		Registry.instance().registerExtension("before.commit", instance);
	}
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		_ITransaction completedTransaction = (_ITransaction)context[0];
		addDocuments((Cache<String,List<Document>>)completedTransaction.getAttribute("lucene.added"));
		updateDocuments((Cache<String,List<Document>>)completedTransaction.getAttribute("lucene.updated"));
		removeDocuments((Cache<String,List<Document>>)completedTransaction.getAttribute("lucene.removed"));
	
	}
	public void addDocuments(Cache<String,List<Document>> documentsByTable){
		if (documentsByTable == null){
			return;
		}
		for (String tableName: documentsByTable.keySet()){
			List<Document> documents = documentsByTable.get(tableName);
			IndexManager.instance().addDocuments(tableName, documents);
		}
	}
	public void updateDocuments(Cache<String,List<Document>> documentsByTable){
		if (documentsByTable == null){
			return;
		}
		for (String tableName: documentsByTable.keySet()){
			List<Document> documents = documentsByTable.get(tableName);
			IndexManager.instance().updateDocuments(tableName, documents);
		}
	}
	public void removeDocuments(Cache<String,List<Document>> documentsByTable){
		if (documentsByTable == null){
			return;
		}
		for (String tableName: documentsByTable.keySet()){
			List<Document> documents = documentsByTable.get(tableName);
			IndexManager.instance().removeDocuments(tableName, documents);
		}
	}
}
