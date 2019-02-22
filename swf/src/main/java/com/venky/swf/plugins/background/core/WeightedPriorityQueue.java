package com.venky.swf.plugins.background.core;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.venky.cache.Cache;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.Task.NormalizedWeightScheme;
import com.venky.swf.plugins.background.core.Task.Priority;
import com.venky.swf.plugins.background.core.Task.PriorityWeightScheme;
import com.venky.swf.routing.Config;

public class WeightedPriorityQueue extends AbstractQueue<Task> {

	private static PriorityWeightScheme getPriorityWeightScheme(){
		String className = Config.instance().getProperty("com.venky.swf.plugins.background.core.priorityWeightScheme.class");
		PriorityWeightScheme scheme = null; 
		
		if (!ObjectUtil.isVoid(className)){
			try {
				scheme = (PriorityWeightScheme) Class.forName(className).newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				scheme = null;
			}
		}
		if (scheme == null) {
			scheme = new PriorityWeightScheme() {};
		}
		return scheme;
	}
	public WeightedPriorityQueue(){
		this(getPriorityWeightScheme());
	}
	
	
	private NormalizedWeightScheme scheme = null;
	
	public WeightedPriorityQueue(PriorityWeightScheme scheme){
		super();
		this.scheme = new NormalizedWeightScheme(scheme);
	}
	private Cache<Priority,Bucket> priorityPollingStatistics = new Cache<Task.Priority, Bucket>() {

		private static final long serialVersionUID = -8600630938699962417L;

		@Override
		protected Bucket getValue(Priority k) {
			return new Bucket();
		}
	};
	private Cache<Priority, Queue<Task>> cache = new Cache<Task.Priority, Queue<Task>>(0,0) {

		private static final long serialVersionUID = 1L;

		@Override
		protected Queue<Task> getValue(Priority k) {
			return new LinkedList<>();
		}
	};
	@Override
	public boolean offer(Task e) {
		return cache.get(e.getTaskPriority()).offer(e);
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
	public Task poll() {
		synchronized (cache) {
			Task polled = null;
			Priority p = getNextPriorityToPoll();
			Queue<Task> q = cache.get(p);
			polled = q.poll();
			if (polled != null) {
				priorityPollingStatistics.get(p).increment();
			}
			return polled;
		}
	}
	@Override
	public Task peek() {
		synchronized (cache) {
			Priority p = getNextPriorityToPoll();
			Queue<Task> q = cache.get(p);
			return q.peek();
		}
	}
	@Override
	public Iterator<Task> iterator() {
		return new Iterator<Task>() {

			@Override
			public boolean hasNext() {
				return WeightedPriorityQueue.this.peek() != null;
			}

			@Override
			public Task next() {
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
