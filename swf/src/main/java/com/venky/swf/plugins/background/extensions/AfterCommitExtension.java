package com.venky.swf.plugins.background.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Transaction;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;

public class AfterCommitExtension implements Extension{
	private static AfterCommitExtension instance = new AfterCommitExtension();
	static {
		Registry.instance().registerExtension("after.commit", instance);
	}
	public void invoke(Object... context) {
		Transaction txn = (Transaction)context[0];
		Boolean trigger = txn.getAttribute(PersistedTaskPollingAgent.class.getName()+".trigger") ;
		if (trigger != null && trigger){
			Agent.instance().start(PersistedTaskPollingAgent.PERSISTED_TASK_POLLER);
		}
	}
	
}
