package com.venky.swf.plugins.background.eventloop;

import java.util.concurrent.atomic.AtomicLong;

public class EventId {
    private static final AtomicLong nextTaskId = new AtomicLong(0);
    public static long next(){
        return nextTaskId.incrementAndGet();
    }
}
