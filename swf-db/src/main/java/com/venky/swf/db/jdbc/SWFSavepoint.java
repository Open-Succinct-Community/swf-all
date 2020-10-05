package com.venky.swf.db.jdbc;

import com.venky.swf.db.Database;
import com.venky.swf.routing.Config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by venky on 22/5/16.
 */
public class SWFSavepoint implements Savepoint{
    private static int savepointId = 0;
    private final String savepointName;
    public SWFSavepoint(String name){
        savepointId++;
        savepointName = name;
    }

    public int getSavepointId() throws SQLException {
        return savepointId;
    }

    public String getSavepointName() throws SQLException {
        return savepointName;
    }

    private final Map<String,Savepoint> savepointMap = new HashMap<>();

    public boolean supportsSavePoints(String pool){
        return Config.instance().getBooleanProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".supportsSavePoints"),true);
    }

    public void addSavepoint(String pool) throws SQLException{
        Connection connection = Database.getInstance().getConnection(pool);
        if (!supportsSavePoints(pool)){
            connection.commit();
            return;
        }

		if (!savepointMap.containsKey(pool)){
	    	if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()){
	            Savepoint pt = connection.setSavepoint();
	            savepointMap.put(pool,pt);
	        }else {
	            Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getEstablishSavepointStatement(getSavepointName())).execute();
	            savepointMap.put(pool,null);
	        }
		}
    }
    public void removeSavepoint(String pool) throws SQLException{
        if (!supportsSavePoints(pool)){
            return;
        }

        Savepoint pt = savepointMap.remove(pool);
        if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()) {
        	if (pt != null) Database.getInstance().getConnection(pool).releaseSavepoint(pt);
        }else {
        	Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getReleaseSavepointStatement(getSavepointName())).execute();
        }
    }

	public void rollback(String pool) throws SQLException {
        Connection connection = Database.getInstance().getConnection(pool);
        if (!supportsSavePoints(pool)){
            connection.rollback();
            return;
        }

        Savepoint pt = savepointMap.remove(pool);
		if (Database.getJdbcTypeHelper(pool).isSavepointManagedByJdbc()) {
            Database.getInstance().getConnection(pool).rollback(pt);
        } else {
            Database.getInstance().createStatement(pool,Database.getJdbcTypeHelper(pool).getRollbackToSavepointStatement(getSavepointName())).execute();
        }
	}

}
