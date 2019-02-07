package com.venky.swf.plugins.background.core;

import java.io.Serializable;

import com.venky.cache.Cache;
import com.venky.core.math.GCDFinder;
import com.venky.swf.routing.Config;



public interface Task extends Serializable , Comparable<Task>{
	public void execute();
	
	default Priority getTaskPriority(){
		return Priority.DEFAULT;
	}
	
	default long getTaskId(){
		return -1L;
	}

	default boolean canExecuteRemotely(){
		return false;
	}
	
	@Override
	default int compareTo(Task o) {
		int ret  = getTaskPriority().compareTo(o.getTaskPriority()); 
		if (ret == 0) {
			ret = Long.compare(getTaskId(), o.getTaskId());
		}
		if (ret == 0) {
			ret = getClass().getName().compareTo(o.getClass().getName());
		}	
		return ret;
	}
	
	public static enum Priority {
		HIGH(-1),
		DEFAULT(0),
		LOW(1);
		private final int value; 

		Priority(final int newValue) {
            value = newValue;
        }

        public int getValue() { return value; }
		
	}
	public static Priority getPriority(int value){ 
		for (Priority p : Priority.values()){ 
			if (value == p.getValue()) {
				return p;
			}
		}
		return Priority.DEFAULT;
	}
	public static interface PriorityWeightScheme { 
		//public int getWeight(Priority priority);
		default public int getWeight(Priority priority){ 
			return Config.instance().getIntProperty("swf.plugins.background.core.Task.Priority."+priority.toString()+".Weight", (2-priority.getValue()));
		}
		
	}
	
	public static class NormalizedWeightScheme implements PriorityWeightScheme{
		
		public NormalizedWeightScheme(PriorityWeightScheme scheme){
			loadNormalizedWeights(scheme);
		}
		Cache<Priority,Integer> cache = new Cache<Task.Priority, Integer>(Priority.values().length,0) {

			private static final long serialVersionUID = 4722723656835020302L;

			@Override
			protected Integer getValue(Priority k) {
				return k.getValue();
			}
		};
		public int getWeight(Priority priority){ 
			return cache.get(priority);
		}
		private void loadNormalizedWeights(PriorityWeightScheme pws){
			int[] wts = new int[]{pws.getWeight(Priority.HIGH),pws.getWeight(Priority.DEFAULT), pws.getWeight(Priority.LOW)};
			int gcd = GCDFinder.getInstance().gcd(wts);
			cache.put(Priority.HIGH,wts[0]/gcd);
			cache.put(Priority.DEFAULT,wts[1]/gcd);
			cache.put(Priority.LOW,wts[2]/gcd);
		}
	}
}
