package com.venky.swf.plugins.background.core.threadpool;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueWrapper<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final Queue<E> queue;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    public BlockingQueueWrapper(Queue<E> queue){
        this(queue,Integer.MAX_VALUE);
    }
    public BlockingQueueWrapper(Queue<E> queue, int capacity) {
        this.queue = Objects.requireNonNull(queue);
        this.capacity = capacity;
    }
    
    @Override
    public boolean add(E e) {
        return offer(e);
    }
    
    @Override
    public boolean offer(E e) {
        lock.lock();
        try {
            if (queue.size() >= capacity) {
                return false;
            }
            boolean added = queue.offer(e);
            if (added) {
                notEmpty.signal();
            }
            return added;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E poll() {
        try {
            return take();
        } catch (InterruptedException e) {
            return null;
        }
    }
    
    @Override
    public E peek() {
        try {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    notEmpty.await();
                }
                E e = queue.peek();
                notFull.signal();
                return e;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            return null;
        }
    }
    
    @Override
    public void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() >= capacity) {
                notFull.await();
            }
            queue.add(e);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (queue.size() >= capacity) {
                if (nanos <= 0L) return false;
                nanos = notFull.awaitNanos(nanos);
            }
            queue.add(e);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E take() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            E e = queue.poll();
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (queue.isEmpty()) {
                if (nanos <= 0L) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            E e = queue.poll();
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int remainingCapacity() {
        lock.lock();
        try {
            return capacity - queue.size();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int drainTo(Collection<? super E> c) {
        lock.lock();
        try {
            int n = 0;
            while (!queue.isEmpty()) {
                c.add(queue.poll());
                n++;
            }
            notFull.signalAll();
            return n;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        lock.lock();
        try {
            int n = 0;
            while (n < maxElements && !queue.isEmpty()) {
                c.add(queue.poll());
                n++;
            }
            if (n > 0) notFull.signalAll();
            return n;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public Iterator<E> iterator() {
        lock.lock();
        try {
            return new ArrayList<>(queue).iterator();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
}
