package com.venky.swf.plugins.background.core;

public interface IOTask extends CoreTask{
    
    @Override
    @SuppressWarnings("unchecked")
    default <W extends AsyncTaskManager> Class<W> getDefaultTaskManagerClass(){
        return (Class<W>)IOTaskManager.class;
    }
    
}
