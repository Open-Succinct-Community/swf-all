package com.venky.swf.plugins.lucene.index.background;

import com.venky.cache.Cache;
import com.venky.core.util.Bucket;
import com.venky.core.util.ChangeListener;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.plugins.lucene.index.background.IndexTask.Operation;
import com.venky.swf.plugins.lucene.index.common.CompleteSearchCollector;
import com.venky.swf.plugins.lucene.index.common.DatabaseDirectory;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.sql.Select;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public DatabaseDirectory getDirectory(String name) {
        return directoryCache.get(name);
    }

    private void _createIndexTasks(String tableName, List<Record> records, Operation operation) {
        List<Document> documents = records.stream().map((Record r)->{
            try {
                Document document = LuceneIndexer.instance(Database.getTable(tableName).getModelClass()).getDocument(r);
                return document;
            }catch(IOException ex){
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
        
        records.clear();

        createIndexTasks(tableName, documents, operation);
    }
    public void createIndexTasks(String tableName, List<Document> documents, Operation operation) {
        List<IndexTask> tasks = new ArrayList<>();
        int batchSize = 50;
        for (Iterator<Document> i = documents.iterator(); i.hasNext();) {
            Document document = i.next();
            IndexTask task ;
            if (tasks.isEmpty() || tasks.get(tasks.size() - 1).getDocuments().size() >= batchSize) {
                task = new IndexTask();
                task.setDirectory(tableName);
                task.setDocuments(new ArrayList<>());
                task.setOperation(operation);
                tasks.add(task);
            } else {
                task = tasks.get(tasks.size() - 1);
            }
            task.getDocuments().add(document);
            i.remove();
        }

        executeDelayed(tasks);
    }

    public void addDocuments(String tableName, List<Record> documents) {
        _createIndexTasks(tableName, documents, Operation.ADD);
    }

    public void updateDocuments(String tableName, List<Record> documents) {
        _createIndexTasks(tableName, documents, Operation.MODIFY);
    }

    public void removeDocuments(String tableName, List<Record> documents) {
        _createIndexTasks(tableName, documents, Operation.DELETE);
    }

    private void executeDelayed(List<IndexTask> tasks) {
        TaskManager.instance().executeAsync(tasks, false);
    }

    private Map<IndexSearcher, Bucket> searcherReferenceCount = new HashMap<>();

    private void incRef(IndexSearcher searcher) {
        synchronized (searcherReferenceCount){
            Bucket bucket = searcherReferenceCount.get(searcher);
            if (bucket == null){
                bucket = new Bucket();
                searcherReferenceCount.put(searcher,bucket);
            }
            bucket.increment();
        }
    }

    private void decRef(String tableName, IndexSearcher searcher) {
        synchronized ( searcherReferenceCount) {
            Bucket bucket = searcherReferenceCount.get(searcher);
            if (bucket == null){
                //Never Should come here.
                bucket = new Bucket();
                searcherReferenceCount.put(searcher,bucket);
            }
            bucket.decrement();
            if (bucket.intValue() <= 0) {
                searcherReferenceCount.remove(searcher);
                IndexSearcher currentSearcher = getIndexSearcher(tableName);
                if (currentSearcher != searcher) {
                    try {
                        searcher.getIndexReader().close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    private IndexSearcher getIndexSearcher(String tableName) {
        try {
            return getDirectory(tableName).getIndexSearcher();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void fire(String tableName, Query q, int numHits,
            ResultCollector callback) {
        IndexSearcher searcher = null;
        try {
            searcher = getIndexSearcher(tableName);
            incRef(searcher);
            if (numHits == Select.MAX_RECORDS_ALL_RECORDS) {
                CompleteSearchCollector collector = new CompleteSearchCollector();
                searcher.search(q, collector);
                for (int docId : collector.getDocIds()) {
                    Document d = searcher.doc(docId);
                    callback.found(d);
                }
            } else {
                int numRecordsFound = 0;
                TopDocs tDocs = searcher.search(q, numHits + 1);
                /*
				TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
				searcher.search(q, collector);
				tDocs = collector.topDocs();
                 */

                ScoreDoc[] hits = tDocs.scoreDocs;
                
                while (numRecordsFound < numHits && tDocs.totalHits > 0) {
                    for (int i = 0; i < hits.length; ++i) {
                        int docId = hits[i].doc;
                        Document d = searcher.doc(docId);
                        if (callback.found(d)){
                            numRecordsFound ++;
                        }
                    }
                    if (numRecordsFound < numHits && tDocs.totalHits > numHits) {
                        tDocs = searcher.searchAfter(hits[hits.length - 1], q, numHits + 1);
                    }else {
                        break;
                    }
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (searcher != null) {
                decRef(tableName, searcher);
            }
        }
    }

}
