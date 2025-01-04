package com.venky.swf.plugins.lucene.extensions;

import com.venky.core.util.MultiException;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db._IDatabase._ITransaction;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.DbTaskManager;
import com.venky.swf.plugins.background.core.IndexTaskManager;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.plugins.lucene.index.background.SWFIndexDirectoryCache;
import com.venky.swf.plugins.lucene.index.common.IndexUpdateTracker;
import com.venky.swf.plugins.lucene.index.common.IndexUpdateTracker.IndexUpdateMeta;
import com.venky.swf.routing.Config;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class LuceneBeforeCommitExtension implements Extension{
	private static LuceneBeforeCommitExtension instance = new LuceneBeforeCommitExtension();
	static {
		Registry.instance().registerExtension("before.commit", instance);
	}
	@SuppressWarnings("unchecked")
	public void invoke(Object... context) {
		_ITransaction completedTransaction = (_ITransaction)context[0];
		IndexUpdateTracker tracker = completedTransaction.getAttribute(IndexUpdateTracker.class.getName());
		if (tracker == null){
			return;
		}
		tracker.getRecordState().forEach((tableName,indexUpdateMetaRecordCache)->{
			List<IndexUpdateMeta> indexUpdateMetaCollection = new ArrayList<>(indexUpdateMetaRecordCache.values());
			// Batching.!!
			LinkedList<LinkedList<IndexUpdateMeta>> batches = batch(indexUpdateMetaCollection);
			for (LinkedList<IndexUpdateMeta> metas : batches) {
				TaskManager.instance().executeAsync(new TableRecordSetIndexer(tableName, metas), false);
			}
		});

    }

	private LinkedList<LinkedList<IndexUpdateMeta>> batch(List<IndexUpdateMeta> indexUpdateMetaCollection) {
		LinkedList<LinkedList<IndexUpdateMeta>> batches = new LinkedList<>();
		batches.add(new LinkedList<>());
		int batchSize = 100;
		for (Iterator<IndexUpdateMeta> i = indexUpdateMetaCollection.iterator(); i.hasNext(); ){
			IndexUpdateMeta meta = i.next();
			LinkedList<IndexUpdateMeta> batch = batches.getLast();
			if (batch.size() >= batchSize){
				batch = new LinkedList<>();
				batches.add(batch);
			}
			batch.add(meta);
			i.remove();
		}

		return batches;
	}


	public static class TableRecordSetIndexer implements Task {
		String tableName ;
		List<IndexUpdateMeta> indexUpdateMetaCollection;

		public TableRecordSetIndexer(String tableName , List<IndexUpdateMeta> indexUpdateMetaCollection){
			this.tableName = tableName;
			this.indexUpdateMetaCollection = indexUpdateMetaCollection;
		}



		private IndexWriter getIndexWriter()  {
			return SWFIndexDirectoryCache.getInstance().getIndexWriter(tableName);
		}
		private Object getWriterLock(){
			return SWFIndexDirectoryCache.getInstance().getWriterLock(tableName);
		}

		@Override
		public void execute() {
			LuceneIndexer indexer = LuceneIndexer.instance(Objects.requireNonNull(Database.getTable(tableName)).getModelClass());
			IndexWriter w = getIndexWriter();
			try {
				Object writerLock = getWriterLock();

				synchronized (writerLock) {
					for (IndexUpdateMeta meta : indexUpdateMetaCollection) {
						Document document = indexer.getDocument(meta.getFinalState());
						switch (meta.getOperation()) {
							case added:
								w.addDocument(document);
								break;
							case updated:
								w.deleteDocuments(new Term("ID", document.getField("ID").stringValue()));
								w.addDocument(document);
								break;
							case removed:
								w.deleteDocuments(new Term("ID", document.getField("ID").stringValue()));
								break;
						}
					}
					w.prepareCommit();
					w.commit();
				}
			}catch (Exception ex){
				MultiException mex = new MultiException();
				mex.add(ex);
				if (w != null){
					try {
						w.rollback();
					}catch (Exception e){
						mex.add(e);
					}
				}
				throw mex;
			}

		}

		@Override
		@SuppressWarnings("unchecked")
		public <W extends AsyncTaskManager> Class<W> getDefaultTaskManagerClass() {
			return (Class<W>)(IndexTaskManager.class);
		}
	}
}
