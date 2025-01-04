package com.venky.swf.plugins.background.core;

import com.venky.swf.db.Database;

public interface DbTask extends CoreTask{
    @Override
    default void onStart() {
        Database.getInstance().getCurrentTransaction();
    }

    @Override
    default void onException(Throwable ex) {
        Database.getInstance().getCurrentTransaction().rollback(ex);
    }

    @Override
    default void onSuccess() {
        Database.getInstance().getCurrentTransaction().commit();
    }

    @Override
    default void onComplete() {
        Database.getInstance().close();
    }

    @Override
    @SuppressWarnings("unchecked")
    default <W extends AsyncTaskManager> Class<W> getDefaultTaskManagerClass() {
        return (Class<W>)DbTaskManager.class;
    }
}
