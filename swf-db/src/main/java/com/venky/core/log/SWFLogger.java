package com.venky.core.log;

import java.util.logging.Logger;

import com.venky.core.log.BetterLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.routing.Config;

public class SWFLogger extends BetterLogger{

	public SWFLogger(Logger logger) {
		super(logger);
	}
	public Timer startTimer(){
		return startTimer(null);
	}
	public Timer startTimer(String ctx){
		return startTimer(ctx,Config.instance().isTimerAdditive());
	}
	
}
