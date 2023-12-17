package com.venky.swf.plugins.lucene.index.background;

import com.venky.swf.plugins.lucene.index.background.SWFIndexDirectoryCache.TrackedSearcher;
import com.venky.swf.plugins.lucene.index.common.CompleteSearchCollector;
import com.venky.swf.plugins.lucene.index.common.ResultCollector;
import com.venky.swf.sql.Select;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

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



    public void fire(String tableName, Query q, int numHits,
            ResultCollector callback) {
        TrackedSearcher searcher = null;
        try {
            searcher = SWFIndexDirectoryCache.getInstance().getIndexSearcher(tableName);
            searcher.open();
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
                
                while (numRecordsFound < numHits && tDocs.totalHits.value > 0) {
                    for (ScoreDoc hit : hits) {
                        int docId = hit.doc;
                        Document d = searcher.doc(docId);
                        if (callback.found(d)) {
                            numRecordsFound++;
                        }
                    }
                    if (numRecordsFound < numHits && tDocs.totalHits.value > numHits) {
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
                searcher.close();
            }
        }
    }

}
