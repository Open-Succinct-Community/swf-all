package com.venky.swf.plugins.background.core.agent;

import com.venky.swf.plugins.background.core.Task;

import java.io.Serial;

public class AgentFinishUpTask implements Task{
	private String agentName;
	private Priority priority  = Priority.DEFAULT;
	public void setPriority(Priority priority){
		this.priority = priority;
	}
	@Override
	public Priority getTaskPriority(){
		return priority;
	}

	public AgentFinishUpTask(){
		
	}
	public AgentFinishUpTask(String agentName){
		setAgentName(agentName);
	}
	@Serial
	private static final long serialVersionUID = -912134633747814136L;

	@Override
	public void execute() {
		Agent.instance().finish(getAgentName());
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}



}
