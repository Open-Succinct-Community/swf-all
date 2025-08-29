package com.venky.swf.plugins.background.core;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.logging.Level;

import com.venky.cache.Cache;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.math.GCDFinder;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.MultiException;
import com.venky.swf.routing.Config;



public interface CoreTask extends Serializable , Comparable<CoreTask> , Runnable, Prioritized{
	default void onStart(){

	}
	default String getTaskIdentifier(){
		return getClass().getName();
	}
	
	@Override
	default void run() {
		Timer timer = Config.instance().getLogger(getTaskIdentifier()).startTimer();
		try {
			
			onStart();
			execute();
			onSuccess();
		}catch (Exception ex){
			StringWriter sw = new StringWriter();
			PrintWriter p = new PrintWriter(sw);
			ExceptionUtil.getRootCause(ex).printStackTrace(p); p.close();
			Config.instance().getLogger(getTaskIdentifier()).log(Level.WARNING,"Task encountered exception " + sw);
			onException(ex);
		}finally{
			timer.stop();
			try {
				Config.instance().getLogger(getTaskIdentifier()).log(Level.INFO,"Completed Task:" + getTaskIdentifier());
				onComplete();
				TimerStatistics.dumpStatistics();
			}catch(Exception ex) {
				Config.instance().getLogger(getTaskIdentifier()).log(Level.WARNING,"Exception while completing task." , ex);
			}
		}
	}
	
	public void execute();

	default void onSuccess(){

	}
	default void onException(Throwable ex){

	}
	default void onComplete(){

	}
	
	default Priority getTaskPriority(){
		return Priority.DEFAULT;
	}
	
	default long getTaskId(){
		return -1L;
	}

	
	@Override
	default int compareTo(CoreTask o) {
		int ret  = getTaskPriority().compareTo(o.getTaskPriority()); 
		if (ret == 0) {
			ret = Long.compare(getTaskId(), o.getTaskId());
		}
		if (ret == 0) {
			ret = getClass().getName().compareTo(o.getClass().getName());
		}	
		return ret;
	}

	@SuppressWarnings("unchecked")
	default AsyncTaskManager getAsyncTaskManager(){
		//return AsyncTaskManagerFactory.getInstance().get(IndexTaskManager.class);
		String className = Config.instance().getProperty(String.format("%s.manager.class",getClass().getName()));
		if (className == null){
			return AsyncTaskManagerFactory.getInstance().get(getDefaultTaskManagerClass());
		}
		
		try {
			return AsyncTaskManagerFactory.getInstance().get((Class<? extends AsyncTaskManager>)Class.forName(className));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	default <W extends AsyncTaskManager> Class<W> getDefaultTaskManagerClass(){
		return (Class<W>)AsyncTaskManager.class;
	}

	public enum Priority {
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
		Cache<Priority,Integer> cache = new Cache<>(Priority.values().length,0) {

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
