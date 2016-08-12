package com.venky.swf.plugins.background.core;

import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;

public class WakeupTask implements Task{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1312407882811140602L;

	@Override
	public void execute() {
		DelayedTaskManager.instance().wakeUp();
	}

}
