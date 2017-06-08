package com.venky.swf.plugins.background.core.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;

public abstract class AgentSeederTask implements Task{
	private static final long serialVersionUID = 3385669580336293058L;
	@Override
	public void execute() {
		if (!Agent.instance().isRunning(getAgentName())){
			return;
		}
		AgentFinishUpTask finish = new AgentFinishUpTask(getAgentName());
		try {
			List<? extends Task> tasks = getTasks();
			if (tasks.isEmpty()) {
				executeDelayed(finish);
			}else {
				executeDelayed(tasks);
			}
		}catch(Exception ex){
			executeDelayed(finish);
		}
	}
	public <T extends Task> void executeDelayed(T task) {
		executeDelayed(Arrays.asList(task));
	}
	public <T extends Task>void executeDelayed(Collection<T> tasks) {
		TaskManager.instance().executeAsync(tasks,isAgentTaskQPersistent());
		if (isAgentTaskQPersistent()){ 
			Agent.instance().start(new PersistedTaskPollingAgent.PersistedTaskPoller());
		}
	}

	public abstract <T extends Task> List<T> getTasks();
	public abstract String getAgentName();
	protected abstract boolean isAgentTaskQPersistent();

}
