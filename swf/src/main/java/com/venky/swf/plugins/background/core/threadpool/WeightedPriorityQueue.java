package com.venky.swf.plugins.background.core.threadpool;

import com.venky.cache.UnboundedCache;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.CoreTask.NormalizedWeightScheme;
import com.venky.swf.plugins.background.core.CoreTask.Priority;
import com.venky.swf.plugins.background.core.CoreTask.PriorityWeightScheme;
import com.venky.swf.routing.Config;

import java.lang.reflect.Method;
import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WeightedPriorityQueue extends AbstractQueue<Runnable>  {
	
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
	
	
	
	private final NormalizedWeightScheme scheme ;
	
	public WeightedPriorityQueue(PriorityWeightScheme scheme){
		super();
		this.scheme = new NormalizedWeightScheme(scheme);
	}
	private final UnboundedCache<Priority,Bucket> priorityPollingStatistics = new UnboundedCache<>(true) {
		@Override
		protected Bucket getValue(Priority k) {
			return new Bucket();
		}
	};
	private final UnboundedCache<Priority, Queue<Runnable>> cache = new UnboundedCache<>(true) {
		@Override
		protected Queue<Runnable> getValue(Priority k) {
			return new ConcurrentLinkedQueue<>();
		}
	};
	
	
	
	private Runnable next(boolean remove) {
		Priority p = getNextPriorityToPoll();
		Queue<Runnable> q = cache.get(p);
		Runnable polled ;
		if (remove) {
			 polled = q.poll();
			if (polled != null) {
				priorityPollingStatistics.get(p).increment();
				Config.instance().getLogger(getClass().getName()).info("Num of %s priority tasks pending: %d".formatted(p, size()));
			}
		}else {
			polled  = q.peek();
		}
		return polled;
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
            ret = Objects.requireNonNullElse(firstNonEmptyQueue, Priority.HIGH);
		}
		return ret;
	}
	
	
	
	@Override
	public boolean offer(Runnable runnable) {
		try {
			Method method = runnable.getClass().getMethod("getTaskPriority");
			return cache.get((Priority) method.invoke(runnable)).offer(runnable);
		}catch (Exception ex){
			return cache.get(Priority.DEFAULT).offer(runnable);
		}
		
	}
	
	@Override
	public Runnable poll() {
		return next(true);
	}
	
	@Override
	public Runnable peek() {
		return next(false);
	}
	
	@Override
	public Iterator<Runnable> iterator() {
		Map<Priority,Iterator<Runnable>> iteratorMap = new HashMap<>();
		for (Priority p : Priority.values()) {
			iteratorMap.putIfAbsent(p,cache.get(p).iterator());
		}
		final UnboundedCache<Priority,Bucket> priorityIteratingStatistics = new UnboundedCache<>(true) {
			@Override
			protected Bucket getValue(Priority k) {
				return new Bucket();
			}
		};
		
		return new Iterator<>() {
			private Priority getNextPriorityToIterate(){
				Priority ret = null;
				Priority firstNonEmptyQueue = null;
				for (Priority p : Priority.values()) {
					if (iteratorMap.get(p).hasNext()){
						if (firstNonEmptyQueue == null){
							firstNonEmptyQueue = p;
						}
						if (priorityIteratingStatistics.get(p).intValue() < scheme.getWeight(p)){
							ret = p;
							break;
						}
					}
				}
				if (ret == null) {
					priorityIteratingStatistics.clear();
                    ret = Objects.requireNonNullElse(firstNonEmptyQueue, Priority.HIGH);
				}
				return ret;
				
			}
		
		
			@Override
			public boolean hasNext() {
				return iteratorMap.get(getNextPriorityToIterate()).hasNext();
			}
			
			@Override
			public Runnable next() {
				return iteratorMap.get(getNextPriorityToIterate()).next();
			}
		};
	}
	
	@Override
	public int size() {
		Bucket size = new Bucket();
		
		for (Priority p : Priority.values()) {
			size.increment(cache.get(p).size());
		}
		return size.intValue();
	}
}
