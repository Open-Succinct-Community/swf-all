package com.venky.swf.plugins.background.core;

import java.io.Serializable;

import com.venky.cache.Cache;
import com.venky.core.math.GCDFinder;



public interface Task extends Serializable{
	public void execute();
	
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
	public static interface PriorityWeightScheme { 
		//public int getWeight(Priority priority);
		
		default public int getWeight(Priority priority){ 
			return 1 - (priority.getValue());
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
