package com.venky.swf.plugins.background.extensions;

import com.venky.core.collections.SequenceSet;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.plugins.background.core.AsyncTaskManager;
import com.venky.swf.plugins.background.core.AsyncTaskManagerFactory;
import com.venky.swf.plugins.background.core.CoreTask;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;

import java.util.Set;

public class InMemoryTaskQueueManager implements Extension {
    private static InMemoryTaskQueueManager instance = new InMemoryTaskQueueManager();
    public static InMemoryTaskQueueManager getInstance(){
        return instance;
    }
    static {
        Registry.instance().registerExtension("after.commit", instance);
    }
    public static void pushTasksToInMemoryTaskQueue(Transaction txn) {
        Set<CoreTask> tasks = getPendingTasks(txn);
        if (tasks.isEmpty()) {
            return;
        }
        AsyncTaskManagerFactory.getInstance().addAll(tasks);
        tasks.clear();
    }
    public static Set<CoreTask> getPendingTasks(){
        return getPendingTasks(Database.getInstance().getCurrentTransaction());
    }
    private static Set<CoreTask> getPendingTasks(Transaction txn){
        Set<CoreTask> tasks = txn.getAttribute(InMemoryTaskQueueManager.class.getName() + ".tasks.to.invoke");
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
