package com.venky.swf.plugins.background.core.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;

public abstract class AgentSeederTask implements Task{
	private static final long serialVersionUID = 3385669580336293058L;
	@Override
	public void execute() {
		if (!Agent.instance().isRunning(getAgentName())){
			return;
		}
		try {
			List<Task> tasks = getTasks();
			if (tasks.isEmpty()) {
				finish();
			}else {
				executeDelayed(tasks);
			}
		}catch(Exception ex){
			Config.instance().getLogger(getClass().getName()).log(Level.WARNING, "Failed To seed Agent", ex);
			finish();
		}
	}
	public <T extends Task> void executeDelayed(T task) {
		executeDelayed(Arrays.asList(task));
	}
	public <T extends Task>void executeDelayed(Collection<T> tasks) {
		TaskManager.instance().executeAsync(tasks,isAgentTaskQPersistent());
	}

	public abstract List<Task> getTasks();
	public abstract String getAgentName();

	protected final boolean isAgentTaskQPersistent() {
		return Config.instance().getBooleanProperty("Agent."+getAgentName() +".isAgentTaskQPersistent",false);
	}

	private AgentFinishUpTask finishUpTask = null;
	public AgentFinishUpTask getFinishUpTask(){
	    if (finishUpTask != null){
	        return finishUpTask;
        }
	    synchronized (this){
	        if (finishUpTask == null){
	            finishUpTask = new AgentFinishUpTask(getAgentName(),canExecuteRemotely());
	            finishUpTask.setPriority(getTaskPriority());
            }
        }
        return finishUpTask;

    }
	public void finish(){
	    getFinishUpTask().execute();
    }

	@Override
	public boolean canExecuteRemotely() {
		return isAgentTaskQPersistent();
	}
}
