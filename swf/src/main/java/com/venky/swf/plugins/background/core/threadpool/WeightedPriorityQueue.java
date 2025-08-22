package com.venky.swf.plugins.background.core.threadpool;

import com.venky.cache.UnboundedCache;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.CoreTask.NormalizedWeightScheme;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.CoreTask.PriorityWeightScheme;
import com.venky.swf.plugins.background.core.threadpool.WeightedPriorityQueueVirtualThreadExecutor.FutureTask;
import com.venky.swf.routing.Config;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class WeightedPriorityQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();
	
	@Override
	public boolean add( Runnable coreTask) {
		return super.add(coreTask);
	}
	
	private static PriorityWeightScheme getPriorityWeightScheme(){
		String className = Config.instance().getProperty("com.venky.swf.plugins.background.core.priorityWeightScheme.class");
		PriorityWeightScheme scheme = null;
		
		if (!ObjectUtil.isVoid(className)){
			try {
				scheme = (PriorityWeightScheme) Class.forName(className).getConstructor().newInstance();
			} catch (Exception e) {
				//
			}
		}
		if (scheme == null) {
			scheme = new PriorityWeightScheme() {};
		}
		return scheme;
	}
	public 	WeightedPriorityQueue(){
		this(getPriorityWeightScheme());
	}
	
	
	private NormalizedWeightScheme scheme = null;
	
	public WeightedPriorityQueue(PriorityWeightScheme scheme){
		super();
		this.scheme = new NormalizedWeightScheme(scheme);
	}
	private final UnboundedCache<Priority,Bucket> priorityPollingStatistics = new UnboundedCache<Priority, Bucket>() {
		@Override
		protected Bucket getValue(Priority k) {
			return new Bucket();
		}
	};
	private final UnboundedCache<Priority, Queue<Runnable>> cache = new UnboundedCache<>() {
		@Override
		protected Queue<Runnable> getValue(Priority k) {
			return new LinkedList<>();
		}
	};
	
	@Override
	public boolean offer( Runnable e) {
		
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			boolean added = cache.get(((FutureTask<?>)e).getPriority()).offer(e);
			notEmpty.signal();
			return added;
		}finally {
			lock.unlock();
		}
	}
	
	@Override
	public void put( Runnable coreTask) throws InterruptedException {
		offer(coreTask);
	}
	
	@Override
	public boolean offer(Runnable coreTask, long timeout, TimeUnit unit) throws InterruptedException {
		return offer(coreTask);
	}
	
	
	@Override
	public  Runnable take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		Runnable result;
		try {
			while ( (result = dequeue()) == null)
				notEmpty.await();
		} finally {
			lock.unlock();
		}
		return result;
		
	}
	
	private Runnable dequeue() {
		Priority p = getNextPriorityToPoll();
		Queue<Runnable> q = cache.get(p);
		Runnable polled = q.poll();
		if (polled != null) {
			priorityPollingStatistics.get(p).increment();
			Config.instance().getLogger(getClass().getName()).info("Num of %s priority tasks pending: %d".formatted(p,size()));
		}
		return polled;
	}
	
	@Override
	public Runnable poll(long timeout,  TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		Runnable result;
		try {
			while ( (result = dequeue()) == null && nanos > 0)
				nanos = notEmpty.awaitNanos(nanos);
		} finally {
			lock.unlock();
		}
		return result;
	}
	
	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}
	
	@Override
	public int drainTo( Collection<? super Runnable> c) {
		return drainTo(c,Integer.MAX_VALUE);
	}
	
	@Override
	public int drainTo( Collection<? super Runnable> c, int maxElements) {
		Objects.requireNonNull(c);
		if (c == this)
			throw new IllegalArgumentException();
		if (maxElements <= 0)
			return 0;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = Math.min(size(), maxElements);
			for (int i = 0; i < n; i++) {
				c.add(_peek()); // In this order, in case add() throws.
				dequeue();
			}
			return n;
		} finally {
			lock.unlock();
		}
	}
	
	private Priority getNextPriorityToPoll(){
		Priority ret = null;
		Priority firstNonEmptyQueue = null;
		for (Priority p : Priority.values()) {
			if (!cache.get(p).isEmpty()){
				if (firstNonEmptyQueue == null){
					firstNonEmptyQueue = p;
				}
				if (priorityPollingStatistics.get(p).intValue() < scheme.getWeight(p)){
					ret = p;
					break;
				}
			}
		}
		if (ret == null) {
			priorityPollingStatistics.clear();
			if (firstNonEmptyQueue != null) {
				ret = firstNonEmptyQueue;
			}else {
				ret = Priority.HIGH;
			}
		}
		return ret;
	}
	@Override
	public Runnable poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return dequeue();
		}finally {
			lock.unlock();
		}
	}
	@Override
	public Runnable peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return _peek();
		} finally {
			lock.unlock();
		}
	}
	private Runnable _peek(){
		Priority p = getNextPriorityToPoll();
		Queue<Runnable> q = cache.get(p);
		return q.peek();
	}
	@Override
	public  Iterator<Runnable> iterator() {
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return WeightedPriorityQueue.this.peek() != null;
			}

			@Override
			public Runnable next() {
				return WeightedPriorityQueue.this.poll() ;
			}
		};
	}
	@Override
	public int size() {
		int size = 0 ;
		List<Priority> priorityList = new ArrayList<>(cache.keySet());
		for (Priority p : priorityList){
			size+= cache.get(p).size();
		}
		return size;
	}
	
	
}
