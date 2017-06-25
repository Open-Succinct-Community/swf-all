package com.venky.swf.plugins.background.core.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.AgentStatus;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

public class Agent {
	public static class Status {  
		boolean running; 
		public void setRunning(boolean running){
			this.running = running;
		}
		public boolean isRunning(){ 
			return this.running;
		}
	}
	private Cache<String,Status> localAgentStatus = new Cache<String, Agent.Status>(0,0) {
		private static final long serialVersionUID = 3597037802654592107L;

		@Override
		protected Status getValue(String k) {
			return new Status();
		}
	}; 
	
	private Map<String,AgentSeederTaskBuilder> agentSeederTaskBuilderMap = new HashMap<String, AgentSeederTaskBuilder>();
	public void registerAgentSeederTaskBuilder(String agentName,AgentSeederTaskBuilder builder){
		synchronized (this) {
			if (builder == null){
				throw new NullPointerException("Trying to register null builder for agent " + agentName);
			}
			agentSeederTaskBuilderMap.put(agentName, builder);
		}
	}
	public void validateAgent(String agentName){
		if (!agentSeederTaskBuilderMap.containsKey(agentName)){
			throw new UnsupportedOperationException(agentName + " Is not a registered agent");
		}
	}
	public AgentSeederTask getAgentSeederTask(String agentName){
		AgentSeederTaskBuilder builder  = agentSeederTaskBuilderMap.get(agentName) ;
		if (builder == null) {
			synchronized (this) {
				builder = agentSeederTaskBuilderMap.get(agentName);
			}
		}
		if (builder == null){
			throw new RuntimeException("Don't know how to seed Task for agent " + agentName);
		}
		return builder.createSeederTask();
	}
	
	public void start(String agentName){
		start(getAgentSeederTask(agentName));
	}
	
	private Agent(){
		
	}
	
	public AgentStatus getStatus(String name){ 
		ModelReflector<AgentStatus> ref = ModelReflector.instance(AgentStatus.class);
		List<AgentStatus> list = new Select(true).from(AgentStatus.class).where(new Expression(ref.getPool(), "NAME" , Operator.EQ, name)).execute(AgentStatus.class);
		AgentStatus status = null;
		if (!list.isEmpty()){
			status = list.get(0);
		}else {
			status = Database.getTable(AgentStatus.class).newRecord();
			status.setName(name);
			status.setRunning(false);
			status.save();
		}
		return status;
	}
	public boolean isRunning(String name){
		AgentSeederTask task = getAgentSeederTask(name);
		return isRunning(task);
	}
	public boolean isRunning(AgentSeederTask task){
		if (!task.isAgentTaskQPersistent()){
			synchronized (localAgentStatus) {
				Status lstatus = localAgentStatus.get(task.getAgentName());
				return lstatus.isRunning();
			}
		}else {
			return getStatus(task.getAgentName()).isRunning();
		}
	}
	public void setRunning(String name,boolean running){
		setRunning(getAgentSeederTask(name), running);
	}
	public void setRunning(AgentSeederTask task,boolean running){
		if (!task.isAgentTaskQPersistent()){
			synchronized (localAgentStatus) {
				Status lstatus = localAgentStatus.get(task.getAgentName());
				lstatus.setRunning(running);
			}
		}else {
			AgentStatus status = getStatus(task.getAgentName());
			status.setRunning(running);
			status.save();
		}
	}
	
	
	public void start(AgentSeederTask  task) {
		if (ObjectUtil.isVoid(task.getAgentName())) {
			throw new NullPointerException(task.getClass().getName() + " doesnot seem to implement getAgentName()");
		}
		boolean start = false;
		if (!isRunning(task)){
			start = true;
			setRunning(task, true);
		}
		if (start){ 
			TaskManager.instance().executeAsync(task,task.isAgentTaskQPersistent());
		}else {
			Config.instance().getLogger(getClass().getName()).info("Agent " + task.getAgentName() + " is already runnning!");
		}
	}
	
	public void finish(String name) {
		validateAgent(name);
		setRunning(name, false);
	}
	
	private static Agent _instance = null;
	public static Agent instance(){ 
		if (_instance == null) {
			synchronized (Agent.class) {
				if (_instance == null) {
					_instance = new Agent();
				}
			}
		}
		return _instance;
	}
}
