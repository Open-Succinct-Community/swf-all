package com.venky.swf.plugins.background.project;

import com.venky.swf.plugins.background.core.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Project {
    public Project(){

    }
    private ExecutorService defaultPool = Executors.newWorkStealingPool();

    List<Future> futures = new ArrayList<>();
    public void submit(List<Task> tasks){
        for (Task task : tasks){
            futures.add(defaultPool.submit(task::execute,null));
        }
    }
    public void awaitCompletion(){
        try {
            futures = Collections.unmodifiableList(futures);
            for (Future<?> future : futures) {
                future.get();
            }
        }catch (Exception ex){
            cancel();
            throw new RuntimeException(ex);
        }finally{
            defaultPool.shutdown();
        }
    }
    public void cancel(){
        for (Future<?> future : futures){
            future.cancel(false);
        }
    }
}
