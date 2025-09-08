package com.venky.swf.plugins.background.core.threadpool;

import com.venky.core.util.Bucket;
import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.TaskHolder;
import com.venky.swf.routing.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrackedExecutorServiceManager {
    Map<Priority,ExecutorService> serviceMap = new ConcurrentHashMap<>();
    Bucket pending = new Bucket();
    
    private static volatile TrackedExecutorServiceManager sSoleInstance;
    
    public static TrackedExecutorServiceManager getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (TrackedExecutorServiceManager.class) {
                if (sSoleInstance == null)
                    sSoleInstance = new TrackedExecutorServiceManager();
            }
        }
        
        return sSoleInstance;
    }
    
    //Make singleton from serialize and deserialize operation.
    protected TrackedExecutorServiceManager readResolve() {
        return getInstance();
    }
    
    
    private TrackedExecutorServiceManager(){
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
        for (Priority value : Priority.values()) {
            //serviceMap.putIfAbsent(value, Executors.newWorkStealingPool(10));
            serviceMap.putIfAbsent(value,Executors.newVirtualThreadPerTaskExecutor());
        }
    }
    
    public ExecutorService getExecutorService(Priority priority){
        return serviceMap.get(priority);
    }
    
    public boolean isShutdown(){
        boolean ret = true;
        for (ExecutorService value : serviceMap.values()) {
            ret = ret && value.isShutdown();
        }
        return ret;
    }
    public boolean isTerminated(){
        boolean ret = true;
        for (ExecutorService value : serviceMap.values()) {
            ret = ret && value.isTerminated();
        }
        return ret;
    }
    
    public void shutdown(){
        for (ExecutorService value : serviceMap.values()) {
            value.shutdown();
        }
    }
    public  <R extends CoreTask> Future<?> submit(R runnable){
        return serviceMap.get(runnable.getTaskPriority()).submit(new TrackedRunnable<>((runnable.getClass() != TaskHolder.class ? new TaskHolder(runnable) : runnable) ,pending));//To ensure taskId comes.
    }
    
    public static Logger getLogger(){
        return Config.instance().getLogger(TrackedExecutorServiceManager.class.getName());
    }
    
    
    private static class TrackedRunnable<R extends CoreTask> implements Runnable {
        
        @Override
        public void run() {
            try {
                runnable.run();
            }finally {
                pending.decrement();
                this.logStatus();
            }
        }
        public void logStatus(){
            getLogger().log(Level.INFO, "( Class: %s , Id :%d , ThreadId %s , Priority : %s , Num Tasks Pending : %d )".formatted(
                    runnable.getClass().getName(),
                    runnable.getTaskId(),
                    Thread.currentThread().threadId(),
                    runnable.getTaskPriority().name(), pending.intValue()));
            
        }
        
        
        R runnable ;
        public TrackedRunnable(R runnable, Bucket pending){
            this.runnable = runnable;
            this.pending = pending;
            pending.increment();
        }
        final Bucket pending ;
    }
    
    public int count() {
        return pending.intValue();
    }
}
