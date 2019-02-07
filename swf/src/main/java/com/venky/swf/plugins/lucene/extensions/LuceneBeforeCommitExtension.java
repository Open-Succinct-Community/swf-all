package com.venky.swf.plugins.lucene.extensions;

import com.venky.cache.Cache;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db._IDatabase._ITransaction;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.lucene.index.agents.IndexUpdatorAgent;
import com.venky.swf.plugins.lucene.index.background.IndexManager;
import org.apache.lucene.document.Document;

import java.util.List;

public class LuceneBeforeCommitExtension implements Extension{
	private static LuceneBeforeCommitExtension instance = new LuceneBeforeCommitExtension();
	static {
		Registry.instance().registerExtension("before.commit", instance);
	}
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		_ITransaction completedTransaction = (_ITransaction)context[0];
		boolean fireAgent = false;
		fireAgent = addDocuments((Cache<String,List<Record>>)completedTransaction.getAttribute("lucene.added")) || fireAgent;
		fireAgent = updateDocuments((Cache<String,List<Record>>)completedTransaction.getAttribute("lucene.updated")) || fireAgent;
		fireAgent = removeDocuments((Cache<String,List<Record>>)completedTransaction.getAttribute("lucene.removed")) || fireAgent;
		if (fireAgent) {
            Agent.instance().start(new IndexUpdatorAgent.IndexUpdatorAgentSeeder());
        }
    }
	public boolean addDocuments(Cache<String,List<Record>> documentsByTable){
		boolean documentsSubmittedForUpdate = false;
        if (documentsByTable == null){
            return documentsSubmittedForUpdate;
        }
		for (String tableName: documentsByTable.keySet()){
			List<Record> documents = documentsByTable.get(tableName);
			IndexManager.instance().addDocuments(tableName, documents);
            documentsSubmittedForUpdate = true;
		}
		return documentsSubmittedForUpdate;

	}
	public boolean updateDocuments(Cache<String,List<Record>> documentsByTable){
        boolean documentsSubmittedForUpdate = false;
        if (documentsByTable == null){
            return documentsSubmittedForUpdate ;
        }
		for (String tableName: documentsByTable.keySet()){
			List<Record> documents = documentsByTable.get(tableName);
			IndexManager.instance().updateDocuments(tableName, documents);
			documentsSubmittedForUpdate = true;
		}
		return documentsSubmittedForUpdate;
	}
	public boolean removeDocuments(Cache<String,List<Record>> documentsByTable){
        boolean documentsSubmittedForUpdate = false;
		if (documentsByTable == null){
			return documentsSubmittedForUpdate;
		}
		for (String tableName: documentsByTable.keySet()){
			List<Record> documents = documentsByTable.get(tableName);
			IndexManager.instance().removeDocuments(tableName, documents);
			documentsSubmittedForUpdate = true;
		}
		return documentsSubmittedForUpdate;
	}
}
