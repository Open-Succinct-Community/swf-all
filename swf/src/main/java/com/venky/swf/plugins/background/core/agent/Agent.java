package com.venky.swf.plugins.background.core.agent;

import java.io.ObjectInputStream;
import java.util.List;

import com.venky.cache.Cache;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.db.model.DelayedTask;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Select;

public class Agent {
	public static class Status {
		public Status(){
			running = false;
		}
		private boolean running;
		public void setRunning(boolean running){
			this.running = running;
		}
		public boolean isRunning(){
			return running;
		}
	}
	private Cache<String,Status> agentStatus = new Cache<String,Status>(0,0){

		private static final long serialVersionUID = 1412815088813902855L;

		@Override
		protected Status getValue(String k) {
			return new Status();
		}
		
	};
	private Agent(){
		List<DelayedTask> tasks = new Select().from(DelayedTask.class).execute();
		for (DelayedTask dt: tasks){
			ObjectInputStream is;
			try {
				is = new ObjectInputStream(dt.getData());
				Task task = (Task)is.readObject();
				is.close();
				if (AgentSeederTask.class.isInstance(task)){
					AgentSeederTask ast = (AgentSeederTask)task;
					agentStatus.get(ast.getAgentName()).setRunning(true);
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public boolean isRunning(String name){
		Status status = agentStatus.get(name);
		synchronized (status) {
			return status.isRunning();
		}
	}
	public void start(AgentSeederTask  task) {
		if (ObjectUtil.isVoid(task.getAgentName())) {
			throw new NullPointerException(task.getClass().getName() + " doesnot seem to implement getAgentName()");
		}
		Status status = agentStatus.get(task.getAgentName());
		synchronized (status) {
			if (!status.isRunning()){
				status.setRunning(true);
				TaskManager.instance().executeAsync(task,task.isAgentTaskQPersistent());
			}else {
				Config.instance().getLogger(getClass().getName()).info("Agent " + task.getAgentName() + " is already runnning!");
			}
		}
	}
	
	public void finish(String name) {
		Status status = agentStatus.get(name);
		synchronized (status) {
			status.setRunning(false);
		}
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