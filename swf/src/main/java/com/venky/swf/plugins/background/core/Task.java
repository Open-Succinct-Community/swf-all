package com.venky.swf.plugins.background.core;

import com.venky.swf.db.Database;

public interface Task extends CoreTask{
    default  boolean isDatabaseAccessed(){
        return true;
    }
    @Override
    default void onStart() {
        if (isDatabaseAccessed()) {
            Database.getInstance().getCurrentTransaction();
        }
    }
    
    @Override
    default void onException(Throwable ex) {
        if (isDatabaseAccessed()) {
            Database.getInstance().getCurrentTransaction().rollback(ex);
        }
    }
    
    @Override
    default void onSuccess() {
        if (isDatabaseAccessed()) {
            Database.getInstance().getCurrentTransaction().commit();
        }
    }
    
    @Override
    default void onComplete() {
        if (isDatabaseAccessed()) {
            Database.getInstance().close();
        }
    }
    
}
