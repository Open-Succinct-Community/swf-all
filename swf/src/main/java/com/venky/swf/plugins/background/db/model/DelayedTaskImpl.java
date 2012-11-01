package com.venky.swf.plugins.background.db.model;

import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import com.venky.core.io.StringReader;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.db.table.Record;
import com.venky.swf.plugins.background.core.Task;

public class DelayedTaskImpl extends ModelImpl<DelayedTask> implements Comparable<DelayedTask>{

	public DelayedTaskImpl(DelayedTask proxy) {
		super(proxy);
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int compareTo(DelayedTask o2) {
		int ret = 0 ;
		Record r1 = getProxy().getRawRecord();
		Record r2 = o2.getRawRecord();
		
		for (String field: DelayedTask.DEFAULT_ORDER_BY_COLUMNS){
			if (ret == 0){
				Comparable v1 = (Comparable)r1.get(field);
				Comparable v2 = (Comparable)r2.get(field);
				
				ret = v1.compareTo(v2);
			}
		}
		
		return 0;
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
					ex.printStackTrace(w);
					Logger.getLogger(getClass().getName()).info(ex.getMessage());
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
