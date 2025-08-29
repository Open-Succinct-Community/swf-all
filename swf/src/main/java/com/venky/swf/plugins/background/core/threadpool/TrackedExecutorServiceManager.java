package com.venky.swf.plugins.background.core.threadpool;

import com.venky.core.util.Bucket;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.Prioritized;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TrackedExecutorServiceManager {
    Map<Priority,ExecutorService> serviceMap = new ConcurrentHashMap<>();
    Bucket pending = new Bucket();
    
    public TrackedExecutorServiceManager(){
        for (Priority value : Priority.values()) {
            serviceMap.putIfAbsent(value, Executors.newVirtualThreadPerTaskExecutor());
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
    public  <R extends Runnable  & Prioritized> Future<?> submit(R runnable){
        return serviceMap.get(runnable.getTaskPriority()).submit(new TrackedRunnable<>(runnable,pending));
    }
    
    private static class TrackedRunnable<R extends Runnable & Prioritized> implements Runnable , Prioritized{
        
        @Override
        public void run() {
            try {
                runnable.run();
            }finally {
                pending.decrement();
            }
        }
        
        
        R runnable ;
        public TrackedRunnable(R runnable, Bucket pending){
            this.runnable = runnable;
            this.pending = pending;
            pending.increment();
        }
        final Bucket pending ;
        
        @Override
        public Priority getTaskPriority() {
            return runnable.getTaskPriority();
        }
    }
    
    public int count() {
        return pending.intValue();
    }
}
