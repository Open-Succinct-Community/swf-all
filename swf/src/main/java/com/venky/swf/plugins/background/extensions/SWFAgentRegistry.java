package com.venky.swf.plugins.background.extensions;

import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;

public class SWFAgentRegistry {
	static { 
		Agent.instance().registerAgentSeederTaskBuilder("PERSISTED_TASK_POLLER", new PersistedTaskPollingAgent());
	}
}
