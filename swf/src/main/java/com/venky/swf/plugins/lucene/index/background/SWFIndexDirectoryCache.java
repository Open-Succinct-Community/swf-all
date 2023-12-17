package com.venky.swf.plugins.lucene.index.background;

import com.venky.cache.UnboundedCache;
import com.venky.swf.routing.Config;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;

public class SWFIndexDirectoryCache  {
    private static volatile SWFIndexDirectoryCache sSoleInstance;

    //private constructor.
    private SWFIndexDirectoryCache() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static SWFIndexDirectoryCache getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (SWFIndexDirectoryCache.class) {
                if (sSoleInstance == null) sSoleInstance = new SWFIndexDirectoryCache();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected SWFIndexDirectoryCache readResolve() {
        return getInstance();
    }

    private final UnboundedCache<String,SWFIndexDirectory> indexDirectoryCache = new UnboundedCache<>() {
        @Override
        protected SWFIndexDirectory getValue(String tableName) {
            try {
                return new SWFIndexDirectory(tableName);
            }catch (IOException ex){
                throw new RuntimeException(ex);
            }
        }
    };

    public IndexWriter getIndexWriter(String tableName){
        return indexDirectoryCache.get(tableName).writer;
    }

    public TrackedSearcher getIndexSearcher(String tableName){
        return indexDirectoryCache.get(tableName).getSearcher();
    }

    private static class SWFIndexDirectory {
        final Directory directory;
        final IndexWriter writer;
        final String tableName ;
        final Object writerLock = new Object();
        final ArrayList<TrackedReader> readers = new ArrayList<>();

        private SWFIndexDirectory(String tableName) throws IOException {
            Config.instance().getLogger(getClass().getName()).log(Level.INFO,"Index Directory " + tableName + " opened by ... ",new RuntimeException("This stack trace"));
            this.tableName = tableName;
            this.directory = initializeDirectory(tableName);
            this.writer = new IndexWriter(directory, newConfig());
            readers.add(new TrackedReader(DirectoryReader.open(writer)));
        }




        private static IndexWriterConfig newConfig(){
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer(new CharArraySet(new ArrayList<>(), false)));
            config.setCommitOnClose(true);
            config.setMergeScheduler(new SerialMergeScheduler());
            return config;
        }
        private static Directory initializeDirectory(String tableName) throws IOException {
            File dir = new File(Config.instance().getProperty("swf.index.dir", ".index"), tableName);
            boolean created = dir.mkdirs();
            Path path = dir.toPath();
            return FSDirectory.open(path);
        }

        public Object getWriterLock(){
            return writerLock;
        }

        public TrackedSearcher getSearcher(){
            try {
                return new TrackedSearcher(getTrackedReader());
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

        public TrackedReader getTrackedReader() throws IOException {
            TrackedReader retReader = null;
            boolean newReader = false;
            for (Iterator<TrackedReader> i = readers.iterator(); i.hasNext() && retReader == null; ) {
                TrackedReader trackedReader = i.next();
                if (i.hasNext()) {
                    if (trackedReader.refCount() == 0 || trackedReader.ageMillis() > 60000L) { //Default Time out 60 Seconds for a search
                        i.remove();
                        trackedReader.reader.close();
                    }
                    // else Wait till references are dropped.
                } else if (!i.hasNext()) {
                    DirectoryReader changedReader = DirectoryReader.openIfChanged(trackedReader.reader);
                    if (changedReader == null || changedReader == trackedReader.reader) {
                        retReader = trackedReader;
                    } else {
                        retReader = new TrackedReader(changedReader);
                        newReader = true;
                    }
                }
            }
            if (retReader == null){
                retReader = new TrackedReader(DirectoryReader.open(writer));
                newReader = true;
            }
            if (newReader) {
                readers.add(retReader);
            }
            return retReader;
        }


    }
    public Object getWriterLock(String tableName){
        return indexDirectoryCache.get(tableName).writerLock;
    }

    public static class TrackedSearcher extends IndexSearcher {

        final TrackedReader trackedReader;
        boolean isOpen = false;
        public TrackedSearcher(TrackedReader trackedReader){
            super(trackedReader.reader);
            this.trackedReader = trackedReader;
        }

        public void open(){
            if (!isOpen) {
                trackedReader.incRef();
                isOpen = true;
            }
        }

        public void close(){
            if (isOpen) {
                trackedReader.decRef();
                isOpen = false;
            }
        }
    }

    public static class TrackedReader  {
        DirectoryReader reader;
        int refCount = 0;
        long updatedAt =0L;
        public TrackedReader(DirectoryReader reader){
            this.reader = reader;
        }

        void incRef(){
            synchronized (this) {
                refCount++;
                updatedAt = System.currentTimeMillis();
            }
        }
        void decRef(){
            synchronized (this) {
                refCount--;
                updatedAt = System.currentTimeMillis();
            }
        }

        public long ageMillis(){
            synchronized (this) {
                return System.currentTimeMillis() - updatedAt;
            }
        }
        public int refCount(){
            synchronized (this) {
                return refCount;
            }
        }
    }

}
