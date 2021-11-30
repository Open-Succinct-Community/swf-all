package com.venky.swf.plugins.background.project;

import com.venky.swf.integration.api.Call;
import com.venky.swf.plugins.background.core.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Project {
    private final String id;

    public String getId() {
        return id;
    }


    public enum Status {
        CREATED,
        STARTED,
        COMPLETED,
        CANCELLED
    }
    private Status status;
    Project(String id){
        this.id = id;
        this.status = Status.CREATED;
    }
    public Status getStatus(){
        return status;
    }
    private final ExecutorService defaultPool = Executors.newWorkStealingPool();

    List<Future<?>> futures = new ArrayList<>();
    public void submit(List<Task> tasks){
        for (Task task : tasks){
            add(task);
        }
        ProjectManager.instance().notify(this);
    }

    public void add(Task task){
        add(() -> {
            task.execute();
            return null;
        });
    }
    public <T> void add(Callable<T> callable){
        futures.add(defaultPool.submit(callable));
    }

    public List<Future<?>> getFutures(){
        return Collections.unmodifiableList(futures);
    }


    public void awaitCompletion(){
        try {
            this.status = Status.STARTED;
            futures = Collections.unmodifiableList(futures);
            for (Future<?> future : futures) {
                future.get();
            }
            this.status = Status.COMPLETED;
        }catch (Exception ex){
            cancel(false);
            throw new RuntimeException(ex);
        }finally{
            shutdown();
        }
    }
    public void cancel(boolean shutdown){
        try {
            for (Future<?> future : futures) {
                future.cancel(false);
            }
        }finally {
            this.status = Status.CANCELLED;
            if (shutdown){
                shutdown();
            }
        }
    }
    public void shutdown(){
        defaultPool.shutdown();
        ProjectManager.instance().notify(this);
    }
}
