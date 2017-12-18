package com.venky.swf.plugins.background.extensions;

import com.venky.core.collections.SequenceSet;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;

import java.util.Set;

public class InMemoryTaskQueueManager implements Extension {
    private static InMemoryTaskQueueManager instance = new InMemoryTaskQueueManager();
    static {
        Registry.instance().registerExtension("after.commit", instance);
    }
    public static void pushTasksToInMemoryTaskQueue(Transaction txn) {
        Set<Task> tasks = getPendingTasks(txn);
        if (tasks.isEmpty()) {
            return;
        }
        AsyncTaskManager.getInstance().addAll(tasks);
        tasks.clear();
    }
    public static Set<Task> getPendingTasks(){
        return getPendingTasks(Database.getInstance().getCurrentTransaction());
    }
    private static Set<Task> getPendingTasks(Transaction txn){
        Set<Task> tasks = txn.getAttribute(InMemoryTaskQueueManager.class.getName() + ".tasks.to.invoke");
        if (tasks == null ) {
            tasks = new SequenceSet<>();
            txn.setAttribute(InMemoryTaskQueueManager.class.getName()  +".tasks.to.invoke", tasks);
        }
        return tasks;
    }

    @Override
    public void invoke(Object... context) {
        Transaction txn = (Transaction) context[0];
        pushTasksToInMemoryTaskQueue(txn);
    }

}
