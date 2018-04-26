package com.venky.swf.plugins.lucene.index.common;

import com.venky.swf.db.Database;
import com.venky.swf.exceptions.SWFTimeoutException;
import com.venky.swf.plugins.lucene.db.model.IndexDirectory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

import java.io.IOException;

public class DatabaseLockFactory extends LockFactory {
    private int id = -1;
    public DatabaseLockFactory(IndexDirectory directory){
        this.id = directory.getId();
    }

    @Override
    public Lock makeLock(String lockName) {
        return new Lock() {
            @Override
            public boolean obtain() throws IOException {
                try {
                    IndexDirectory directory = Database.getTable(IndexDirectory.class).lock(id);
                    return directory != null;
                }catch (SWFTimeoutException ex){
                    return  false;
                }
            }

            @Override
            public void release() throws IOException {
                // Will happen on commit any way.
            }

            @Override
            public boolean isLocked() throws IOException {
                IndexDirectory directory = Database.getTable(IndexDirectory.class).get(id);
                return directory.getRawRecord().isLocked();
            }
        };
    }

    @Override
    public void clearLock(String lockName) throws IOException {
        // Locks all are cleared on commit.
    }
}
