package com.venky.swf.plugins.lucene.index.background;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

import com.venky.cache.Cache;
import com.venky.core.util.Bucket;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.sql.Select;

public class IndexManager {
	private static IndexManager _instance = new IndexManager();

	public static IndexManager instance() {
		return _instance;
	}

	private IndexManager() {

	}

	private Cache<String, Directory> directoryCache = new Cache<String, Directory>() {
		@Override
		protected Directory getValue(String k) {
			try {
				return new DatabaseDirectory(k);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	};
	
	private Cache<String, WriterDaemon> writerDelegate = new Cache<String, WriterDaemon>() {
		@Override
		protected WriterDaemon getValue(String k) {
			WriterDaemon daemon = new WriterDaemon(directoryCache.get(k));
			daemon.setDaemon(true);
			daemon.start();
			return daemon;
		}
	};
	public void addDocuments(String tableName, List<Document> documents){
		writerDelegate.get(tableName).addDocuments(documents);
	}

	public void updateDocuments(String tableName, List<Document> documents) {
		writerDelegate.get(tableName).updateDocuments(documents);
	}

	public void removeDocuments(String tableName, List<Document> documents) {
		writerDelegate.get(tableName).removeDocuments(documents);
	}

	private Cache<String, IndexSearcher> indexSearcherCache = new Cache<String, IndexSearcher>() {
		
		@Override
		protected IndexSearcher getValue(String k) {
			try {
				return new IndexSearcher(
						IndexReader.open(directoryCache.get(k)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private Cache<IndexSearcher, Bucket> searcherReferenceCount = new Cache<IndexSearcher, Bucket>() {
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
			IndexSearcher currentSearcher = indexSearcherCache.get(tableName);
			if (currentSearcher != searcher){
				try {
					searcher.getIndexReader().close();
					searcher.close();
				}catch (IOException ex){
					throw new RuntimeException(ex);
				}
			}
		}
	}

	private IndexSearcher getIndexSearcher(String tableName) {
		try {
			IndexSearcher searcher = null;
			synchronized (indexSearcherCache) {
				searcher = indexSearcherCache.get(tableName);
				IndexReader newReader = IndexReader.openIfChanged(searcher.getIndexReader());
				if (newReader != null) {
					searcher = new IndexSearcher(newReader);
					indexSearcherCache.put(tableName, searcher);
				}
			}
			return searcher;
		} catch (IOException ex) {
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
				TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
				searcher.search(q, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

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
