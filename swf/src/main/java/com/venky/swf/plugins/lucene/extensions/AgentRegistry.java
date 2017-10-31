package com.venky.swf.plugins.lucene.extensions;

import com.venky.swf.plugins.background.core.agent.Agent;
import com.venky.swf.plugins.lucene.index.agents.IndexUpdatorAgent;

public class AgentRegistry {
    static {
        Agent.instance().registerAgentSeederTaskBuilder(IndexUpdatorAgent.INDEX_UPDATOR_AGENT,new IndexUpdatorAgent());
    }
}
