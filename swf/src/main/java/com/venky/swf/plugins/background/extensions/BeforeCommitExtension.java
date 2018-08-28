package com.venky.swf.plugins.background.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.db.Transaction;
import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.background.core.agent.PersistedTaskPollingAgent;

public class BeforeCommitExtension implements Extension{
	private static BeforeCommitExtension instance = new BeforeCommitExtension();
	static {
		Registry.instance().registerExtension("before.commit", instance);
	}
	public void invoke(Object... context) {
		Transaction txn = (Transaction)context[0];
		String triggerAgent = PersistedTaskPollingAgent.class.getName()+".trigger" ;
        Registry.instance().callExtensions("before." + triggerAgent,txn);

        Boolean trigger = txn.getAttribute(triggerAgent) ;
        if (trigger != null && trigger) {
            Agent.instance().start(PersistedTaskPollingAgent.PERSISTED_TASK_POLLER);
        }

	}

}