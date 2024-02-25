package com.venky.swf.plugins.background.eventloop;

import com.venky.core.util.MultiException;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.TaskManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public  class CoreEvent implements CoreTask {
    long id = -1;
    @Override
    public long getTaskId() {
        return id;
    }

    CoreFuture proxy= null;
    CoreEvent parent = null;
    public CoreEvent(){
        this(null,null);
    }
    private CoreEvent(CoreEvent parent,CoreFuture proxy){
        this.proxy = proxy;
        if (proxy != null){
            this.id = proxy.getTaskId();
        }
        if (this.id < 0){
            this.id = EventId.next();
        }
        this.parent = parent;
    }

    public CoreFuture getFuture(){
        return proxy;
    }

    private CoreEvent createChild(CoreFuture child){
        return new CoreEvent(this,child);
    }

    public boolean isReady(){
        synchronized (this){
            boolean ready = true;
            for (Map.Entry<Long,CoreEvent> entry : childEvents.entrySet()){
                ready = ( results.containsKey(entry.getKey()) || exceptionMap.containsKey(entry.getKey()) ) && entry.getValue().isReady();
                if (!ready){
                    break;
                }
            }
            return ready;
        }
    }

    public void onChildSuccess(CoreEvent child){
        synchronized (this){
            CoreFuture future = child.getFuture();
            Object value = future == null ? null : future.get();
            results.put(child.getTaskId(), value);
        }
    }

    public void onChildException(CoreEvent child, Throwable ex){
        synchronized (this) {
            exceptionMap.put(child.getTaskId(), ex);
        }
    }
    public void onChildComplete(CoreEvent child){
        if (isReady()) {
            // Last child is completed
            getAsyncTaskManager().addAll(Collections.singleton(this));
        }
    }

    public boolean hasExceptions(){
        synchronized (this) {
            return !exceptionMap.isEmpty();
        }
    }

    @Override
    public void execute() {
        CoreEvent.setCurrentEvent(this); //Thread local global needed for spawning child events
        if (proxy != null) {
            proxy.execute();
        }
    }


    @Override
    public void onStart() {
        if  (hasExceptions()){
            MultiException multiException = new MultiException();
            exceptionMap.values().forEach(e->multiException.add(e));
            throw multiException;
        }
        CoreTask.super.onStart();
        if (proxy != null ){
            proxy.onStart();
        }
    }

    @Override
    public void onSuccess() {
        if (proxy != null){
            proxy.onSuccess();
        }
        CoreTask.super.onSuccess();
        if (parent != null) {
            parent.onChildSuccess(this);
        }
    }
    @Override
    public void onComplete() {
        if (!isReady()){
            // All children spawned  are not completed.
            //this is not complete
            return;
        }
        if (proxy != null){
            proxy.onComplete();
        }
        CoreTask.super.onComplete();
        if (parent != null ){
            parent.onChildComplete(this);
        }

    }
    @Override
    public void onException(Throwable ex) {
        if (proxy != null){
            proxy.onException(ex);
        }
        CoreTask.super.onException(ex);
        if (parent != null){
            parent.onChildException(this,ex);
        }
    }


    final Map<Long,Object> results = new HashMap<>();
    final Map<Long,Throwable> exceptionMap = new HashMap<>();
    final Map<Long,CoreEvent> childEvents = new HashMap<>();
    public void spawn(CoreFuture... children){
        spawn(true,children);
    }
    public void spawn(CoreEvent... childrenEvent){
        spawn(true,childrenEvent);
    }
    public void spawn(boolean immediate,CoreFuture... children){
        List<CoreEvent> events = new ArrayList<>();
        for (CoreFuture child : children){
            CoreEvent childEvent = createChild(child);
            events.add(childEvent);
        }
        spawn(immediate,events.toArray(new CoreEvent[]{}));
    }
    public void spawn(boolean immediate, CoreEvent... childrenEvent){
        synchronized (this){
            for (CoreEvent childEvent: childrenEvent){
                if (childEvent.parent == null){
                    childEvent.parent = this;
                }
                if (childEvent.parent != this){
                    throw new IllegalArgumentException("Not a child.");
                }
                childEvents.put(childEvent.getTaskId(),childEvent);
            }
        }
        if (immediate){
            AsyncTaskManagerFactory.getInstance().addAll(Arrays.asList(childrenEvent));
        }else {
            TaskManager.instance().executeAsync(Arrays.asList(childrenEvent), false);
        }
    }

    private static final ThreadLocal<CoreEvent> currentEvent = new ThreadLocal<>();
    public static void setCurrentEvent(CoreEvent event){
        currentEvent.set(event);
    }
    public static CoreEvent getCurrentEvent(){
        return currentEvent.get();
    }
    /*
    public static void spawnOff(CoreFuture... children){

        CoreEvent parent = currentEvent.get();
        List<CoreEvent> childEvents = new ArrayList<>();
        for (CoreFuture child: children){
            if (parent != null){
                childEvents.add(parent.createChild(child));
            }else {
                CoreEvent childEvent = new  CoreEvent(null,child);
                childEvents.add(childEvent);
            }
        }
        spawnOff(childEvents.toArray(new CoreEvent[]{}));
    }

     */
    public static void spawnOff(CoreEvent... children){
        spawnOff(true, children);
    }
    public static void spawnOff(boolean immediate,CoreEvent... children){
        CoreEvent parent = currentEvent.get();
        if (parent != null){
            parent.spawn(immediate,children);
        }else {
            if (immediate){
                AsyncTaskManagerFactory.getInstance().addAll(Arrays.asList(children));
            }else {
                TaskManager.instance().executeAsync(Arrays.asList(children), false);
            }
        }
    }
}
