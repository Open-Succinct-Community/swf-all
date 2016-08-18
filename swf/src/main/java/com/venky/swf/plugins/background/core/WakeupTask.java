package com.venky.swf.plugins.background.core;

public class WakeupTask implements Task{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1312407882811140602L;

	@Override
	public void execute() {
		TaskManager.instance().wakeUp();
	}

}
