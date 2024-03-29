package com.venky.swf.db;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.venky.core.checkpoint.Checkpoint;
import com.venky.core.checkpoint.MergeableMap;
import com.venky.core.log.ExtendedLevel;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.swf.db.jdbc.SWFSavepoint;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.routing.Config;

public class Transaction implements _IDatabase._ITransaction {
    public SWFSavepoint getSavepoint() {
        return savepoint;
    }

    private SWFSavepoint savepoint = null;
    private int transactionNo = -1 ;
    private Checkpoint<MergeableMap<String,Object>> checkpoint = null;

    public Transaction(int transactionNo, Checkpoint<MergeableMap<String,Object>> cp) {
        this.transactionNo = transactionNo;
        checkpoint = cp;
        savepoint = new SWFSavepoint(String.valueOf(transactionNo));
        setSavepoint();
        Config.instance().getLogger(Database.class.getName()).fine("Transaction:"+transactionNo+" Started : " + Database.getCaller());
    }

    public int getTransactionNo(){
        return transactionNo;
    }
    public Checkpoint<MergeableMap<String,Object>> getCheckpoint(){
        return checkpoint;
    }

    SWFLogger cat = Config.instance().getLogger(getClass().getName());
    public void setSavepoint(){
        try {
            for (String pool : getActivePools()){ //Set in all pools
            	cat.fine("Set Save point on pool " + pool );
            	savepoint.addSavepoint(pool);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseSavepoint(){
        try {
        	for (String pool :getActivePools()) {
               savepoint.removeSavepoint(pool);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollbackToSavePoint(){
        try {
        	for (String pool : getActivePools()) {
                savepoint.rollback(pool);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void registerCommit() { 
    	Config.instance().getLogger(Database.class.getName()).fine("Transaction:"+transactionNo+" .registerCommit : " + Database.getCaller());
        Database.getInstance().getTransactionManager().registerCommit(this);
    }
    public void commit() {
    	String ctx = cat.isLoggable(ExtendedLevel.TIMER) ? "Transaction:"+transactionNo+ " commit " + Database.getCaller() : null;
    	Timer t = cat.startTimer(ctx,Config.instance().isTimerAdditive());
    	try {
            Database.getInstance().getTransactionManager().commit(this);
    	}finally{ 
    		t.stop();
    	}
	}
    public void rollback(Throwable th) { 
    	String ctx = cat.isLoggable(ExtendedLevel.TIMER) ? "Transaction:"+transactionNo+ " rollback " + Database.getCaller() : null;
    	Timer t = cat.startTimer(ctx,Config.instance().isTimerAdditive());
    	try {
    	    Database.getInstance().getTransactionManager().rollback(this,th);
    	}finally{ 
    		t.stop();
    	}
    }


    public <M extends Model> QueryCache getCache(ModelReflector<M> ref) {
        String tableName = ref.getTableName();

        QueryCache queryCache = (QueryCache)getAttribute(QueryCache.class.getName()+".for."+tableName);

        if (queryCache == null){
            queryCache = new QueryCache(tableName);
        }

        setAttribute(QueryCache.class.getName() + ".for." + tableName, queryCache);
        return queryCache;
    }

    public void setAttribute(String name,Object value){
        checkpoint.getValue().put(name, value);
        if (value != null && !(value instanceof Serializable) && !(value instanceof Cloneable)){
            Config.instance().getLogger(Database.class.getName()).warning(value.getClass().getName() + " not Serializable or Cloneable. Checkpointing in nested transactions may exhibit unexpected behaviour!");
        }
    }

    @SuppressWarnings("unchecked")
    public <A> A getAttribute(String name){
        return (A)checkpoint.getValue().get(name);
    }

    public Map<String,Object> getAttributes(){
        return checkpoint.getValue();
    }
    public void setAttributes(Map<String,Object> values){
        values.forEach(this::setAttribute);
    }

    public Set<String> getActivePools(){
    	Set<String> activePools = getAttribute("active.connection.pools");
    	if (activePools == null){
    		activePools = new HashSet<String>(10);
    		setAttribute("active.connection.pools", activePools);
    	}
    	return activePools;
    }
    public void registerActivePool(String pool){
    	if (getActivePools().add(pool)){
        	try {
    			savepoint.addSavepoint(pool);
    		} catch (SQLException e) {
    			throw new RuntimeException(e);
    		}
    	}
    }
    
    public void registerTableDataChanged(String tableName){
        getTablesChanged().add(tableName);
    }
    public Set<String> getTablesChanged(){
        Set<String> models = getAttribute("tables.modified");
        if (models == null){
            models = new HashSet<String>();
            setAttribute("tables.modified", models);
        }
        return models;
    }
}
