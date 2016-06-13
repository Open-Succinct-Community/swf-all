package com.venky.swf.db;

import com.venky.core.checkpoint.Checkpoint;
import com.venky.core.checkpoint.MergeableMap;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.jdbc.SWFSavepoint;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.routing.Config;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashSet;
import java.util.Set;

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
        setSavepoint(String.valueOf(transactionNo));
        Config.instance().getLogger(Database.class.getName()).fine("Transaction:"+transactionNo+" Started : " + Database.getCaller());
    }

    public int getTransactionNo(){
        return transactionNo;
    }
    public Checkpoint<MergeableMap<String,Object>> getCheckpoint(){
        return checkpoint;
    }

    public void setSavepoint(String name){
        try {
            savepoint = new SWFSavepoint(name);
            for (String pool : ConnectionManager.instance().getPools()){ //Set in all pools
                if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()){
                    Savepoint pt = Database.getInstance().getConnection(pool).setSavepoint(name);
                    savepoint.addSavepoint(pool,pt);
                }else {
                    Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getEstablishSavepointStatement(name)).execute();
                    savepoint.addSavepoint(pool,null);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void releaseSavepoint(SWFSavepoint sp,String name){
        try {
            for (String pool : ConnectionManager.instance().getPools()) {
                if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()) {
                    Database.getInstance().getConnection(pool).releaseSavepoint(sp.removeSavepoint(pool));
                } else {
                    Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getReleaseSavepointStatement(name)).execute();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void rollbackToSavePoint(SWFSavepoint sp,String name){
        try {
            for (String pool : ConnectionManager.instance().getPools()) {
                if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()) {
                    Database.getInstance().getConnection(pool).rollback(sp.removeSavepoint(pool));
                } else {
                    Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getRollbackToSavepointStatement(name)).execute();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        Config.instance().getLogger(Database.class.getName()).fine("Transaction:"+transactionNo+" .commit : " + Database.getCaller());
        Database.getInstance().getTransactionManager().commit(this);

    }
    public void rollback(Throwable th) {
        Config.instance().getLogger(Database.class.getName()).fine("Transaction :" + transactionNo + " Rollback" + ": " + Database.getCaller());
        Database.getInstance().getTransactionManager().rollback(this,th);
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
