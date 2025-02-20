package com.venky.swf.plugins.lucene.index.background;

import com.venky.swf.plugins.lucene.index.background.SWFIndexDirectoryCache.TrackedSearcher;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;

public class IndexManager {

    private static volatile IndexManager sSoleInstance;

    //private constructor.
    private IndexManager() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }


    public static IndexManager getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (IndexManager.class) {
                if (sSoleInstance == null) sSoleInstance = new IndexManager();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected IndexManager readResolve() {
        return getInstance();
    }

    public int count(String tableName , Query q){
        TrackedSearcher searcher = null;
        try  {
            searcher = SWFIndexDirectoryCache.getInstance().getIndexSearcher(tableName);
            searcher.open();
            return searcher.count(q);
        }catch (IOException ex){
            throw new RuntimeException(ex);
        }finally {
            if (searcher != null){
                searcher.close();
            }
        }
    }

    public void fire(String tableName, Query q, int numHits,
            ResultCollector collector) {
        TrackedSearcher searcher = null;
        try {
            searcher = SWFIndexDirectoryCache.getInstance().getIndexSearcher(tableName);
            searcher.open();
            if (numHits == 0){
                TopDocs tDocs = searcher.search(q, Integer.MAX_VALUE);
                ScoreDoc[] hits = tDocs.scoreDocs;
                if (tDocs.totalHits.value > 0) {
                    for (ScoreDoc hit : hits) {
                        int docId = hit.doc;
                        Document d = searcher.storedFields().document(docId);
                        collector.collect(d, hit);
                    }
                }
            }else {
                TopDocs tDocs = searcher.search(q, numHits + 1);
                
                ScoreDoc[] hits = tDocs.scoreDocs;
                
                while (!collector.isEnough() && tDocs.totalHits.value > 0) {
                    for (ScoreDoc hit : hits) {
                        int docId = hit.doc;
                        Document d = searcher.storedFields().document(docId);
                        collector.collect(d, hit);
                    }
                    if (!collector.isEnough() && tDocs.totalHits.value > numHits && hits.length > 0) {
                        tDocs = searcher.searchAfter(hits[hits.length - 1], q, numHits + 1);
                        hits = tDocs.scoreDocs;
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (searcher != null) {
                searcher.close();
            }
        }
    }

}
