package com.venky.swf.plugins.background.db.model;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.venky.core.io.StringReader;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.routing.Config;

public class DelayedTaskImpl extends ModelImpl<DelayedTask> implements Comparable<DelayedTask>{

	public DelayedTaskImpl(DelayedTask proxy) {
		super(proxy);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int compareTo(DelayedTask o2) {
		int ret = 0 ;
		DelayedTask o1 = getProxy();
		ModelReflector<DelayedTask> ref = ModelReflector.instance(DelayedTask.class);
		for (String field: DelayedTask.DEFAULT_ORDER_BY_COLUMNS){
			if (ret == 0){
				Comparable v1 = (Comparable)ref.get(o1,field);
				Comparable v2 = (Comparable)ref.get(o2,field);
				ret = v1.compareTo(v2);
			}
		}
		
		return ret;
	}
	
	public void execute(){
		DelayedTask o = getProxy();
		Transaction parentTxn = Database.getInstance().createTransaction();
		try { 
			DelayedTask locked = Database.getTable(DelayedTask.class).lock(o.getId(),false);
			if (locked != null) {
				boolean success = false;
				Transaction txn  = null;
				try {
					ObjectInputStream is = new ObjectInputStream(locked.getData());
					Task task = (Task)is.readObject();
					txn = Database.getInstance().createTransaction();
					task.execute();
					txn.commit();
					success = true;
				}catch(Exception ex){
					txn.rollback(ex);
					
					StringWriter sw = new StringWriter();
					PrintWriter w = new PrintWriter(sw);
					if (Config.instance().isDevelopmentEnvironment() || ObjectUtil.isVoid(ex.getMessage())){
			            ex.printStackTrace(w);
			        }else {
			        	w.write(ex.getMessage());
			        }
					Config.instance().getLogger(getClass().getName()).info(sw.toString());
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
