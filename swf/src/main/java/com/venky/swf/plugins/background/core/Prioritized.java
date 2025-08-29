package com.venky.swf.plugins.background.core;

import com.venky.swf.plugins.background.core.CoreTask.Priority;

public interface Prioritized {
    Priority getTaskPriority();
}
