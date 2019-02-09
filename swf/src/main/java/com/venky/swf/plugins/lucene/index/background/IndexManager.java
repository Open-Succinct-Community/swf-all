package com.venky.swf.plugins.lucene.index.background;

import com.venky.cache.Cache;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.util.Bucket;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.background.core.SerializationHelper;
import com.venky.swf.plugins.lucene.db.model.IndexQueue;
import com.venky.swf.plugins.lucene.index.background.IndexTask.Operation;
import com.venky.swf.plugins.lucene.index.common.CompleteSearchCollector;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.sql.Select;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class IndexManager {
	private static IndexManager _instance = new IndexManager();

	public static IndexManager instance() {
		return _instance;
	}

	private IndexManager() {

	}

	private Cache<String, DatabaseDirectory> directoryCache = new Cache<String, DatabaseDirectory>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7199535528835102853L;

		@Override
		protected DatabaseDirectory getValue(String k) {
			try {
				return new DatabaseDirectory(k);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	};
	
	public DatabaseDirectory getDirectory(String name){
		return directoryCache.get(name);
	}

	private IndexTask createIndexTask(String tableName,List<Record> documents, Operation operation){
		IndexTask task = new IndexTask();
		task.setDirectory(tableName);
		task.setDocuments(documents);
		task.setOperation(operation);
		return task;
	}
	
	public void addDocuments(String tableName, List<Record> documents){
		executeDelayed(createIndexTask(tableName, documents, Operation.ADD));
	}

	public void updateDocuments(String tableName, List<Record> documents) {
		executeDelayed(createIndexTask(tableName, documents, Operation.MODIFY));
	}

	public void removeDocuments(String tableName, List<Record> documents) {
		executeDelayed(createIndexTask(tableName, documents, Operation.DELETE));
	}

    private void executeDelayed(IndexTask indexTask) {
        IndexQueue q = Database.getTable(IndexQueue.class).newRecord();
        try {
			SerializationHelper helper = new SerializationHelper();

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			helper.write(os,indexTask);
			os.close();
            q.setIndexTask(new ByteArrayInputStream(os.toByteArray()));
            q.save();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }


	private Cache<IndexSearcher, Bucket> searcherReferenceCount = new Cache<IndexSearcher, Bucket>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3704220332108717190L;

		@Override
		protected Bucket getValue(IndexSearcher k) {
			return new Bucket();
		}
	};

	private void incRef(IndexSearcher searcher) {
		Bucket bucket = searcherReferenceCount.get(searcher);
		bucket.increment();
	}

	private void decRef(String tableName,IndexSearcher searcher) {
		Bucket bucket = searcherReferenceCount.get(searcher);
		bucket.decrement();
		if (bucket.intValue() <= 0) {
			IndexSearcher currentSearcher = getIndexSearcher(tableName);
			if (currentSearcher != searcher){
				try {
					searcher.getIndexReader().close();
				}catch (IOException ex){
					throw new RuntimeException(ex);
				}
			}
		}
	}

	private IndexSearcher getIndexSearcher(String tableName) {
		try {
			return getDirectory(tableName).getIndexSearcher();
		}catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}

	public void fire(String tableName, Query q, int numHits,
			ResultCollector callback) {
		IndexSearcher searcher = null;
		try {
			searcher = getIndexSearcher(tableName);
			incRef(searcher);
			if (numHits == Select.MAX_RECORDS_ALL_RECORDS){
				CompleteSearchCollector collector = new CompleteSearchCollector();
				searcher.search(q, collector);
				for (int docId : collector.getDocIds()){
					Document d = searcher.doc(docId);
					callback.found(d);
				}
			}else {
				TopDocs tDocs = searcher.search(q,numHits);
				/*
				TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
				searcher.search(q, collector);
				tDocs = collector.topDocs();
				*/
				ScoreDoc[] hits = tDocs.scoreDocs;

				for (int i = 0; i < hits.length; ++i) {
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
					callback.found(d);
				}
				
			}
			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}finally {
			if (searcher != null){
				decRef(tableName, searcher);
			}
		}
	}

}
