package com.venky.swf.plugins.background.messaging;

import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;

public class EventEmitter{
    private static volatile EventEmitter sSoleInstance;

    //private constructor.
    private EventEmitter() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static EventEmitter getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (EventEmitter.class) {
                if (sSoleInstance == null) sSoleInstance = new EventEmitter();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected EventEmitter readResolve() {
        return getInstance();
    }



    public void emit(String eventName, Object payload ){
        TaskManager.instance().executeAsync((DbTask)()-> Event.find(eventName).raise(payload),false);
    }
}
