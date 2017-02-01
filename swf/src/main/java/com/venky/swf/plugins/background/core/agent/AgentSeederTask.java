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
		List<Task> tasks = getTasks();
		if (tasks.isEmpty()) {
			AgentFinishUpTask task = new AgentFinishUpTask();
			task.setAgentName(getAgentName());
			executeDelayed(task);
		}else {
			executeDelayed(tasks);
		}
	}
	public void executeDelayed(Task task) {
		executeDelayed(Arrays.asList(task));
	}
	public void executeDelayed(Collection<Task> tasks) {
		TaskManager.instance().executeAsync(tasks,isAgentTaskQPersistent());
	}

	public abstract List<Task> getTasks();
	public abstract String getAgentName();
	protected abstract boolean isAgentTaskQPersistent();

}
