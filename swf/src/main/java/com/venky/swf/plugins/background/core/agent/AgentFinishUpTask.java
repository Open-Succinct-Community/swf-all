package com.venky.swf.plugins.background.core.agent;

import com.venky.swf.plugins.background.core.Task;

public class AgentFinishUpTask implements Task{
	private String agentName; 
	
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
