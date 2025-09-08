package com.venky.swf.plugins.background.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public interface _TaskManager {
    public Future<?> submit(Runnable runnable);
    public List<Future<?>> submit(List<Runnable> runnable);
    
    public void submitAsync(Runnable runnable);
    public void submitAsync(List<Runnable> runnables);
    
}
