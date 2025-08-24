package com.venky.swf.plugins.background.core;

import com.venky.cache.Cache;
import com.venky.swf.db.model.status.AsyncServerStatus;
import com.venky.swf.db.model.status.AsyncServerStatus.AsyncServerStatuses;
import com.venky.swf.db.model.status.ServerStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AsyncTaskManagerFactory {
    private static volatile AsyncTaskManagerFactory sSoleInstance;

    //private constructor.
    private AsyncTaskManagerFactory(){
        //Prevent form the reflection api.
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static AsyncTaskManagerFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (AsyncTaskManagerFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new AsyncTaskManagerFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected AsyncTaskManagerFactory readResolve() {
        return getInstance();
    }

    private final Map<Class<? extends AsyncTaskManager>, AsyncTaskManager> taskManagerCache = new HashMap<>();
    public <T extends AsyncTaskManager> T get(Class<T> asyncTaskManagerClazz){
        T t = (T)taskManagerCache.get(asyncTaskManagerClazz);
        if (t != null){
            return t;
        }
        synchronized (taskManagerCache){
            t = (T)taskManagerCache.get(asyncTaskManagerClazz);
            if (t == null){
                try {
                    t = asyncTaskManagerClazz.getConstructor().newInstance();
                    taskManagerCache.put(asyncTaskManagerClazz,t);
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }
        return t;
    }
    public void status(ServerStatus status){
        AsyncServerStatuses asyncServerStatuses = status.getAsyncServerStatuses();
        if (asyncServerStatuses == null){
            asyncServerStatuses = new AsyncServerStatuses();
            status.setAsyncServerStatuses(asyncServerStatuses);
        }
        for (Class<? extends AsyncTaskManager> aClass : taskManagerCache.keySet()) {
            AsyncTaskManager asyncTaskManager = taskManagerCache.get(aClass);
            asyncServerStatuses.add(new AsyncServerStatus(){{
                setAsyncServer(aClass.getSimpleName());
                setJobCount(asyncTaskManager.count());
            }});
        }
    }

    public void shutdown() {
        for (Entry<Class<? extends AsyncTaskManager>, AsyncTaskManager> entry : taskManagerCache.entrySet()) {
            Class<? extends AsyncTaskManager> cz = entry.getKey();
            AsyncTaskManager atm = entry.getValue();
            atm.shutdown();
        }
        taskManagerCache.clear();
    }


    public <T extends CoreTask> void addAll(Collection<T> tasks) {
        Map<AsyncTaskManager,List<CoreTask>> tasksMap = group(tasks);
        tasksMap.forEach((atm,l)->atm.addAll(l));
    }

    private <T extends CoreTask> Map<AsyncTaskManager,List<CoreTask>> group(Collection<T> tasks){
        Map<AsyncTaskManager,List<CoreTask>> tasksMap = new Cache<AsyncTaskManager, List<CoreTask>>() {
            @Override
            protected List<CoreTask> getValue(AsyncTaskManager asyncTaskManager) {
                return new ArrayList<>();
            }
        };

        for (T task: tasks){
            tasksMap.get(task.getAsyncTaskManager()).add(task);
        }

        return tasksMap;
    }
    public <T extends CoreTask> void executeAsync(T task){
        this.executeAsync(List.of(task),false);
    }
    public <T extends CoreTask> void executeAsync(Collection<T> tasks){
        this.executeAsync(tasks,false);
    }
    public <T extends CoreTask> void executeAsync(Collection<T> tasks, boolean persistTaskQueue) {
        Map<AsyncTaskManager,List<CoreTask>> group = group(tasks);
        group.forEach((atm,l)->atm.execute(l,persistTaskQueue));
    }
    
}
