package com.venky.swf.plugins.background.core.task.manager;

import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.CoreTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class  SynchronousTaskManager extends AsyncTaskManager {
    @Override
    public List<Future<?>> addAll(Collection<? extends CoreTask> tasks) {
        List<Future<?>> f = new ArrayList<>();
        tasks.forEach(t->{
            t.run();
            f.add(new Future<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }
                
                @Override
                public boolean isCancelled() {
                    return false;
                }
                
                @Override
                public boolean isDone() {
                    return true;
                }
                
                @Override
                public Object get() throws InterruptedException, ExecutionException {
                    return null;
                }
                
                @Override
                public Object get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return null;
                }
            });
        });
        return f;
    }
   
    
}
