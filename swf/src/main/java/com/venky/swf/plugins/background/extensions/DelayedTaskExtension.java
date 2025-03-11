package com.venky.swf.plugins.background.extensions;

import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.db.model.DelayedTask;

public class DelayedTaskExtension extends ModelOperationExtension<DelayedTask> {
    static {
        registerExtension(new DelayedTaskExtension());
    }
    
    @Override
    protected void beforeValidate(DelayedTask instance) {
        super.beforeValidate(instance);
        if (instance.getTaskClassName() == null) {
            Task task = instance.getContainedTask();
            if (task != null) {
                instance.setTaskClassName(task.getClass().getName());
            }
        }
    }
}
