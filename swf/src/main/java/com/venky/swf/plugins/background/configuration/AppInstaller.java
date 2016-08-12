package com.venky.swf.plugins.background.configuration;

import com.venky.swf.configuration.Installer;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;
public class AppInstaller implements Installer{

  public void install() {
	  TaskManager.instance().wakeUp(); //Bring up the daemon only on commit
  }
}

