package com.venky.core.log;

import com.venky.core.log.TimerStatistics.Timer;

public class TimerUtils {
	@FunctionalInterface
	public static interface Execution<R> {
		public R execute();
		default R wrap(SWFLogger cat, String context) {
			Timer timer = cat.startTimer(context);
			try {
				return execute();
			}finally {
				timer.stop();
			}
		}
	}
	
	public static <R> R time(SWFLogger cat, String context, Execution<R> e){ 
		return e.wrap(cat,context);
	}
}
