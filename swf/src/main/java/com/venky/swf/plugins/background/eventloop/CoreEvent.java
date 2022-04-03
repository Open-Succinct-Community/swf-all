package com.venky.swf.plugins.background.eventloop;

import com.venky.core.util.MultiException;
import com.venky.swf.plugins.background.core.CoreTask;

import java.util.Collections;
import java.util.HashMap;
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
            for (Map.Entry<Long,CoreEvent> entry : childEvents.entrySet()){
                if (!results.containsKey(entry.getKey()) && !exceptionMap.containsKey(entry.getKey())){
                    return false;
                }
            }
            return true;
        }
    }

    public void onChildSuccess(CoreEvent child){
        synchronized (this){
            results.put(child.getTaskId(), child.getFuture().get());
        }
    }

    public void onChildException(CoreEvent child, Throwable ex){
        synchronized (this) {
            exceptionMap.put(child.getTaskId(), ex);
        }
    }
    public void onChildComplete(CoreEvent child){
        if (isReady()){
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
        if (proxy != null){
            proxy.onComplete();
        }
        CoreTask.super.onComplete();
        if (parent != null){
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
    public void spawn(CoreFuture child){
        CoreEvent childEvent = createChild(child);
        synchronized (this){
            childEvents.put(childEvent.getTaskId(),childEvent);
        }
        child.getAsyncTaskManager().addAll(Collections.singleton(childEvent));
    }


}
