package com.venky.swf.plugins.background.core;

public interface IOTask extends CoreTask{
    @Override
    default IOTaskManager getAsyncTaskManager() {
        return AsyncTaskManagerFactory.getInstance().get(IOTaskManager.class);
    }
}
