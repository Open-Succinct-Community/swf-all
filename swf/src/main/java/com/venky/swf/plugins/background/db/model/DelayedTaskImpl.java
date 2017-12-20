package com.venky.swf.plugins.background.db.model;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.Task.Priority;
import com.venky.swf.routing.Config;

public class DelayedTaskImpl extends ModelImpl<DelayedTask> {

	public DelayedTaskImpl(DelayedTask proxy) {
		super(proxy);
	}
	
	public Priority getTaskPriority(){ 
		return Task.getPriority(getProxy().getPriority());
	}
	public int getTaskId(){
		return getProxy().getId();
	}

    public String getTaskClassName() {
	    DelayedTask proxy = getProxy();
	    try {
            ObjectInputStream is = new ObjectInputStream(proxy.getData());
            Task task = (Task) is.readObject();
            is.close();
            return task.getClass().getName();
        }catch (Exception e){
	        return "Unknown";
        }
    }


    public void execute(){
		DelayedTask o = getProxy();
		Transaction parentTxn = Database.getInstance().getTransactionManager().createTransaction();
		try { 
			DelayedTask locked = Database.getTable(DelayedTask.class).lock(o.getId(),false);
			if (locked != null) {
				boolean success = false;
				Transaction txn  = null;
				try {
					ObjectInputStream is = new ObjectInputStream(locked.getData());
					Task task = (Task)is.readObject();
					is.close();
					Config.instance().getLogger(getClass().getName()).info("Executing " + task.getClass().getName() + " : DelayedTask#" + o.getId() );
					txn = Database.getInstance().getTransactionManager().createTransaction();
					task.execute();
					txn.commit();
					success = true;
				}catch(Exception ex){
					StringWriter sw = new StringWriter();
					PrintWriter w = new PrintWriter(sw);
					if (Config.instance().isDevelopmentEnvironment() || ObjectUtil.isVoid(ex.getMessage())){
			            ex.printStackTrace(w);
			        }else {
			        	w.write(ex.getMessage());
			        }
					Config.instance().getLogger(getClass().getName()).info(sw.toString());

					txn.rollback(ex);
					locked.setLastError(new StringReader(sw.toString()));
					locked.setNumAttempts(locked.getNumAttempts()+1);
				}
				if (success){
					locked.destroy();
				}else {
					locked.save();
				}
			}
			parentTxn.commit();
		}catch (Exception ex){
			parentTxn.rollback(ex);
		}
	}
}