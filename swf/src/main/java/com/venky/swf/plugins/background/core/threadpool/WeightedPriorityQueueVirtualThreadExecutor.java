package com.venky.swf.plugins.background.core.threadpool;

import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.CoreTask.Priority;


import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WeightedPriorityQueueVirtualThreadExecutor extends ThreadPoolExecutor {
    public WeightedPriorityQueueVirtualThreadExecutor() {
        super(
                0, Integer.MAX_VALUE, // Dynamic pool size for Virtual Threads
                60L, TimeUnit.SECONDS,
                new WeightedPriorityQueue(),
                Thread.ofVirtual().factory() // Use Virtual Thread factory
        );
    }
    
    
    
    
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }
    
    public static class FutureTask<V> extends java.util.concurrent.FutureTask<V>{
        
        Priority priority ;
        public FutureTask( Runnable runnable,V value) {
            super(runnable, value);
            this.priority = ((CoreTask)runnable).getTaskPriority();
        }
        
        public Priority getPriority(){
            return this.priority;
        }
        
    }
    
    
}
