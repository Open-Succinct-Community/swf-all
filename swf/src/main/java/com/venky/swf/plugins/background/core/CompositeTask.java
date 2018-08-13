package com.venky.swf.plugins.background.core;

import com.venky.core.util.MultiException;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;

import java.util.Iterator;
import java.util.LinkedList;

public class CompositeTask implements Task {
	private final LinkedList<Task> tasks ;
	private static final long serialVersionUID = -6287257141086371584L;
	private boolean splitTasksOnException = false;
	

	public CompositeTask(){
		this.tasks = new LinkedList<>();
	}
	private Priority priority = Priority.LOW;
    public CompositeTask(Task...tasks ) {
        this(false,tasks);
    }
	public CompositeTask(boolean splitTasksOnException , Task...tasks ) {
        this.splitTasksOnException = splitTasksOnException;
		this.tasks = new LinkedList<>(); 
		for (Task t : tasks){
			this.tasks.add(t);
			if (priority.getValue() > t.getTaskPriority().getValue()){ //Lower the value higher the priority.
				priority = t.getTaskPriority();
			}
		}
	}
	@Override
	public Priority getTaskPriority(){
		return priority;
	}
	
	@Override
	public void execute() {
        Transaction parentTxn = Database.getInstance().getTransactionManager().createTransaction();
        try {
            MultiException multiException = new MultiException();
            for (Iterator<Task> taskIterator = tasks.iterator(); taskIterator.hasNext() ; ) {
                Task task = taskIterator.next();
                Transaction txn = Database.getInstance().getTransactionManager().createTransaction();
                try {
                    task.execute();
                    txn.commit();
                    taskIterator.remove();
                }catch (Exception ex){
                    txn.rollback(ex);// Task not removed will be resubmitted if required.
                    multiException.add(ex);
                }
            }

            if (!tasks.isEmpty()) {
                if (splitTasksOnException){
                    TaskManager.instance().executeAsync(tasks,false); //Submit errored tasks for retry. // May throw serializable exception
                }else {
                    throw multiException;
                }
            }
            parentTxn.commit(); //Don't waste work done by successful tasks.
        }catch ( Exception ex ) {
            parentTxn.rollback(ex);
            throw ex;
        }
	}
	
}
