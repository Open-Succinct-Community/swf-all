package com.venky.swf.db.jdbc;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by venky on 22/5/16.
 */
public class SWFSavepoint implements Savepoint{
    private static int savepointId = 0;
    private String savepointName;
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

    private Map<String,Savepoint> savepointMap = new HashMap<String, Savepoint>();

    public void addSavepoint(String pool, Savepoint savepoint){
        savepointMap.put(pool,savepoint);
    }
    public Savepoint removeSavepoint(String pool){
        return savepointMap.remove(pool);
    }

}
