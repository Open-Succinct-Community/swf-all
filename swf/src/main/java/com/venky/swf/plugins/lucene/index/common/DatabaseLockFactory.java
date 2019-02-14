package com.venky.swf.plugins.lucene.index.common;

import com.venky.swf.db.Database;
import com.venky.swf.exceptions.SWFTimeoutException;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import com.venky.swf.plugins.lucene.db.model.IndexFile;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

import java.io.IOException;

public class DatabaseLockFactory extends LockFactory {
    IndexDirectory directory ;
    public IndexDirectory getDirectory(){
        return directory;
    }

    public DatabaseLockFactory(IndexDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Lock obtainLock(Directory dir, String lockName) throws IOException {

        return new Lock() {
            @Override
            public void close() throws IOException {

            }

            @Override
            public void ensureValid() throws IOException {
                try {
                    Database.getTable(IndexDirectory.class).lock(((DatabaseDirectory)dir).getModelDirectory().getId());
                    /*
                    IndexFile file = ((DatabaseDirectory)dir).getFile(lockName);
                    if (file != null){
                        Database.getTable(IndexFile.class).lock(file.getId());
                    }
                    */
                }catch (SWFTimeoutException ex){
                    throw new IOException(ex);
                }
            }
        };
    }
}

