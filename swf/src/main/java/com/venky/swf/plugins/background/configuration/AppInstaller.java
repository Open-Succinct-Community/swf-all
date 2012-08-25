package com.venky.swf.plugins.background.configuration;

import com.venky.swf.configuration.Installer;
import com.venky.swf.plugins.background.core.workers.DelayedTaskManager;
public class AppInstaller implements Installer{

  public void install() {
    DelayedTaskManager.instance(); //Bring up the daemon
  }
}

