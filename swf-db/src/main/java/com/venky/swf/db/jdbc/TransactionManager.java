package com.venky.swf.db.jdbc;

import java.sql.SQLException;
import java.util.Stack;

import com.venky.core.checkpoint.Checkpointed;
import com.venky.core.checkpoint.MergeableMap;
import com.venky.extension.Registry;
import com.venky.swf.db.Database;
import com.venky.swf.db.Transaction;
import com.venky.swf.routing.Config;

/**
 * Created by venky on 22/5/16.
 */
public class TransactionManager {
    private Stack<Transaction> transactionStack = new Stack<Transaction>();
    public Transaction createTransaction() {
        Transaction transaction = new Transaction(transactionStack.size(), txnUserAttributes.createCheckpoint());
        transactionStack.push(transaction);
        return transaction;
    }
    public boolean isActiveTransactionPresent(){
    	return !transactionStack.isEmpty();
    }

    public Transaction getCurrentTransaction(){
        if (transactionStack.isEmpty()) {
            createTransaction();
        }
        return transactionStack.peek();
    }

    public void completeAllTransaction(){
        if (!transactionStack.isEmpty()){
            Config.instance().getLogger(TransactionManager.class.getName()).warning(transactionStack.size() + " Transactions not closed correctly. Recovering.");
            transactionStack.clear();
        }
        txnUserAttributes.rollback(); //All check points are clear.
        txnUserAttributes.getCurrentValue().clear(); // Now restore the initial value to a clear map.
    }

    public void rollback(Transaction transaction, Throwable th) {

        boolean entireTransactionIsRolledBack = false;
        for (String pool : transaction.getActivePools()){
            entireTransactionIsRolledBack = entireTransactionIsRolledBack || Database.getJdbcTypeHelper(pool).hasTransactionRolledBack(th);
        }
        if (!entireTransactionIsRolledBack){
            transaction.rollbackToSavePoint();
        }
        txnUserAttributes.rollback(transaction.getCheckpoint());
        updateTransactionStack(transaction);
        if (transactionStack.isEmpty()){
            try {
                Config.instance().getLogger(Database.class.getName()).fine("Connection Rollback" + ":" +  Database.getCaller());
                for (String pool : transaction.getActivePools()){
                    Database.getInstance().getConnection(pool).rollback();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }else{
            if (entireTransactionIsRolledBack){
                if (RuntimeException.class.isInstance(th)){
                    throw (RuntimeException)th;
                }else {
                    throw new RuntimeException(th);
                }
            }
        }

    }
    public void commit(Transaction transaction){
        transaction.releaseSavepoint();
        transaction.setSavepoint();
        registerCommit(transaction);
    }
    public void registerCommit(Transaction transaction) {
		txnUserAttributes.commit(transaction.getCheckpoint());
        updateTransactionStack(transaction);
        if (transactionStack.isEmpty()){
            try {
                transactionStack.push(transaction);
                Registry.instance().callExtensions("before.commit",transaction);//Still part of current transaction.
                transactionStack.pop();
                Config.instance().getLogger(Database.class.getName()).fine("Connection.commit:" + Database.getCaller());
                for (String pool : transaction.getActivePools()) {
                    Database.getInstance().getConnection(pool,false).commit();
                }
                txnUserAttributes.getCurrentValue().clear(); // Now restore the initial value to a clear map.
                Database.getInstance().registerLockRelease();
                Registry.instance().callExtensions("after.commit",transaction);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
	}
    private void updateTransactionStack(Transaction transaction) {
        Transaction completedTransaction = getCurrentTransaction();
        if (completedTransaction != transaction) {
            throw new RuntimeException("Transaction " + transaction.getTransactionNo() + " Has incomplete nested transactions ");
        }
        transactionStack.pop();
    }



    private Checkpointed<MergeableMap<String,Object>> txnUserAttributes = new Checkpointed<MergeableMap<String,Object>>(new MergeableMap<String, Object>());
	

}
