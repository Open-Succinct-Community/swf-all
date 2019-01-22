/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.venky.cache.Cache;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.jdbc.ConnectionManager;
import com.venky.swf.db.jdbc.TransactionManager;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.TableReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;

/**
 * 
 * @author venky
 */
public class Database implements _IDatabase{

    private Database() {

    }

    private User currentUser ;
	public void open(Object currentUser) {
		if (connectionCache.get(ModelReflector.instance(User.class).getPool()) == null) {
			throw new RuntimeException("Failed to open connection to database: " +  getCaller());
		}
		this.currentUser = (User)currentUser;
	}

    public User getCurrentUser(){
		return currentUser;
	}

    private TransactionManager tm = null;
    public TransactionManager getTransactionManager(){
        if (tm == null){
            tm = new TransactionManager();
        }
        return tm;
    }

    public Transaction getCurrentTransaction() {
        return getTransactionManager().getCurrentTransaction();
    }
    public boolean isActiveTransactionPresent(){ 
    	return getTransactionManager().isActiveTransactionPresent();
    }

    public void close() {
		closeConnections();
		currentUser = null;
		
	}

	private void closeConnections() {
        List<String> pools = new ArrayList<String>(connectionCache.keySet());
        for (String pool : pools){
            Connection connection = connectionCache.get(pool);
            try {
                if (!connection.isClosed()){
                	try {
                		connection.rollback();
                	}catch (SQLException ex){
                        Config.instance().getLogger(Database.class.getName()).fine("Rollback Failed!! Closing anyway to release locks " + getCaller());
                	}finally {
                		connection.close();
                	}
                    Config.instance().getLogger(Database.class.getName()).fine("Connection closed : " + getCaller());
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            } finally {
                connectionCache.remove(pool);
            }
        }
        if (!pools.isEmpty()){
            getTransactionManager().completeAllTransaction();
        }
	}

    private Cache<String,Connection> connectionCache = new Cache<String,Connection>(){
        /**
		 * 
		 */
		private static final long serialVersionUID = 8256289464354769723L;

		protected Connection getValue(String pool) {
            try {
                return createConnection(pool);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    };
    public Set<String> getActivePools(){
    	return connectionCache.keySet();
    }
    
    private boolean registeringActivePool = false; // Prevent Recursion from createTransaction , setSavePoint calling getConnection.
    public Connection getConnection(String pool){
    	return getConnection(pool,true);
    }
    public Connection getConnection(String pool,boolean registerActivePool){
    	Connection conn = connectionCache.get(pool);
    	try {
			if (conn.getTransactionIsolation() != getTransactionIsolationLevel() ){
				if ( conn.getMetaData().supportsTransactionIsolationLevel(getTransactionIsolationLevel())) {
					conn.setTransactionIsolation(getTransactionIsolationLevel());
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		
    	if (registerActivePool && !registeringActivePool){
        	registeringActivePool = true;
        	try {
            	getCurrentTransaction().registerActivePool(pool);
        	}finally {
        		registeringActivePool = false;
        	}
    	}
    	
        return conn;
    }

    public void registerLockRelease(){
        for (QueryCache cache: configQueryCacheMap.values()){
            cache.registerLockRelease();
        }
    }

	public <M extends Model> QueryCache getCache(ModelReflector<M> ref) {
		String tableName = ref.getTableName();
    	if (ref.isAnnotationPresent(CONFIGURATION.class)){
    		QueryCache cacheEntry = configQueryCacheMap.get(tableName);
			if (cacheEntry == null) {
				synchronized (configQueryCacheMap) {
    				cacheEntry = configQueryCacheMap.get(tableName);
					if (cacheEntry == null) {
						cacheEntry = new QueryCache(tableName);
						configQueryCacheMap.put(tableName, cacheEntry);
					}
				}
			}
			return cacheEntry;
		} else {
			return getTransactionManager().getCurrentTransaction().getCache(ref);
		}
	}
    public PreparedStatement createStatement(String pool, String sql) throws SQLException{
        return getConnection(pool).prepareStatement(sql);
    }
    public PreparedStatement createStatement(String pool, String sql,String[] columnNames) throws SQLException{
        return getConnection(pool).prepareStatement(sql, columnNames );
    }

	// Class level methods and variables.
	private static final Map<String, QueryCache> configQueryCacheMap = new HashMap<String, QueryCache>();

	private static Map<String,Map<String, Table<? extends Model>>> tablesInPool = new HashMap<String,Map<String, Table<? extends Model>>>();
	public static Map<String, Table<? extends Model>> getTables(String pool) {
        Map<String,Table<? extends  Model>> map = tablesInPool.get(pool);
        if (map == null){
            map = new IgnoreCaseMap<Table<? extends Model>>();
            tablesInPool.put(pool,map);
        }
		return tablesInPool.get(pool);
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model> Table<M> getTable(Class<M> modelClass) {
		return (Table<M>) getTable(ModelReflector.instance(modelClass).getPool(),Table.tableName(modelClass));
	}
	public static <M extends Model> Table<M> getTable(String table){
    	SequenceSet<Table<M>> set = new SequenceSet<>();
    	
    	for (String pool:tablesInPool.keySet()){
    		Table<M> t = getTable(pool, table);
    		if (t != null){
    			set.add(t);
    			if (t.getModelClass()!= null){
    				return t;
    			}
    		}
    	}
    	if (set.size() > 0){
    		return set.first();
    	}
        return null;
    }

	@SuppressWarnings("unchecked")
	public static <M extends Model> Table<M> getTable(String pool , String tableName) {
		return (Table<M>) tablesInPool.get(pool).get(tableName);
	}

	public static Set<String> getTableNames() {
        Set<String> tableNames = new HashSet<String>();
        for (String pool :tablesInPool.keySet()){
            tableNames.addAll(tablesInPool.get(pool).keySet());
        }
		return tableNames;
	}

	public static void migrateTables() {
		boolean dbModified = false;
		loadTables(dbModified);
        for (String pool: tablesInPool.keySet()) {
            for (Table<?> table : tablesInPool.get(pool).values()) {
            	if (table.isVirtual()){
            		continue;
            	}
                Config.instance().getLogger(Database.class.getName()).info("Table " + table.getRealTableName() + " :" + pool + "Model " + table.getModelClass()    + " :" + table.getPool());
                if (!table.isExistingInDatabase() && ObjectUtil.equals(table.getReflector().getPool(),pool)) {
                    table.createTable();
                    dbModified = true;
                } else if (table.getModelClass() == null || !ObjectUtil.equals(table.getReflector().getPool(),pool)) {
                    table.dropTable();
                    dbModified = true;
                } else {
                    dbModified = table.sync() || dbModified;
                }
            }
        }
		loadTables(dbModified);
	}
	public static void loadTables(boolean reload) {
		if (reload) {
            tablesInPool.clear();
		}
		if (!tablesInPool.isEmpty()) {
			return;
		}
		loadTablesFromModel();
		loadTablesFromDB();
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void loadTablesFromModel() {
        List<String> modelClasses = Config.instance().getModelClasses();

        for (String className : modelClasses) {
            try {
                Class<? extends Model> modelClass = (Class<? extends Model>) Class.forName(className);
                if (!className.equals(Model.class.getName()) && modelClass.isInterface() && Model.class.isAssignableFrom(modelClass)){
                    Table table = new Table(modelClass);
                    table.setExistingInDatabase(false);
                    String tableName = table.getTableName();
                    String pool = ModelReflector.instance(modelClass).getPool();

                    if (table.getRealTableName() != null && !getTables(pool).containsKey(tableName)) {
                        getTables(pool).put(table.getTableName(),table);
                    }
                }
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    private static void loadTablesFromDB() {
        for (String pool : ConnectionManager.instance().getPools()){
            loadTablesFromDB(pool);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadTablesFromDB(String pool) {
		ResultSet tablesResultSet = null;
		try { 
			Connection conn =  getInstance().getConnection(pool);
			DatabaseMetaData meta = conn.getMetaData();
            tablesResultSet = meta.getTables(conn.getCatalog(), getSchema(pool), "%", new String[]{"TABLE"});
			while (tablesResultSet.next()) {
				String tableName = tablesResultSet.getString("TABLE_NAME");
				Table table = getTables(pool).get(tableName);
				if (table == null){ //
					table = new Table(tableName,pool);
                    getTables(pool).put(tableName, table);
				}
				table.setExistingInDatabase(true);
                ResultSet columnResultSet = null; 
                try {
	                columnResultSet = meta.getColumns(null,getSchema(pool), tableName, null);
					while (columnResultSet.next()) {
	                    String columnName  = columnResultSet.getString("COLUMN_NAME");
	                    table.getColumnDescriptor(columnName,true).load(columnResultSet);
					}
                }finally {
                	if (columnResultSet != null){
                		columnResultSet.close();
                	}
                }
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}finally {
			if (tablesResultSet != null){
				try {
					tablesResultSet.close();
				} catch (SQLException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}
	public void evictTableFromPool(String pool,String tableName){
		//If app determines that a table is dropped (like archived) etc. this may be called.
		getTables(pool).remove(tableName);
	}

	public void loadFactorySettings() {
    	List<String> installerNames = Config.instance().getInstallers();
		try {
			for (String installerName : installerNames){
				Config.instance().getLogger(Database.class.getName()).info("Installing ... " + installerName );
				try{
					Installer installer = (Installer)Class.forName(installerName).newInstance();
					installer.install();
				}finally {
					Config.instance().getLogger(Database.class.getName()).info("done!");
				}
			}
			Database.getInstance().getCurrentTransaction().commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}


	private static ThreadLocal<Database> _instance = new ThreadLocal<Database>();

    public static Database getInstance() {
        return getInstance(false);
    }

    public static Database getInstance(boolean migrate) {
		if (_instance.get() == null) {
			Database db = new Database();
			_instance.set(db);
		}
		if (migrate) {
			migrateTables();
		} else {
			loadTables(false);
		}
		return _instance.get();
	}

	public static JdbcTypeHelper getJdbcTypeHelper(String pool) {
		return ConnectionManager.instance().getJdbcTypeHelper(pool);
	}
	
	private static String getSchema(String pool) {
		return Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+ pool + ".dbschema"));
	}

	private static boolean isSchemaToBeSetOnConnection(String pool) {
		return Boolean.valueOf(Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".dbschema.setonconnection")));
	}
	
    private static Connection createConnection(String pool) throws SQLException, ClassNotFoundException{
		Connection conn = ConnectionManager.instance().createConnection(pool);
		conn.setAutoCommit(false);
		if (isSchemaToBeSetOnConnection(pool)) {
			String schemaSettingCommand = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+ pool +".set.dbschema.command"), "set schema ?");
			PreparedStatement stmt = conn.prepareStatement(schemaSettingCommand);
			if (schemaSettingCommand.indexOf('?') >= 0) {
				stmt.setString(1, getSchema(pool));
			}
			stmt.executeUpdate();
			conn.commit();
		}
		Config.instance().getLogger(Database.class.getName()).fine("Opened Connection:" + getCaller());
		return conn;
	}

	public static void shutdown() {
		ConnectionManager.instance().close();
		for (String pool:ConnectionManager.instance().getPools()){
			String jdbcurl = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".url"));
			String driver = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+ ".driver"));
			if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
				try {
					jdbcurl = jdbcurl + ";shutdown=true";
	                String userid = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc."+pool+".userid"));
	                String password = Config.instance().getProperty(ConnectionManager.instance().getNormalizedPropertyName("swf.jdbc." + pool + ".password"));
	                Properties info = new Properties();
					info.setProperty("user", userid);
					info.setProperty("password", password);
					DriverManager.getConnection(jdbcurl, info);
				} catch (SQLException ex) {
	                if (ex.getSQLState().equals("08006") && ex.getErrorCode() == 45000){
						System.out.println("Derby db closed!");
					} else {
						throw new RuntimeException(ex);
					}
				}
			}
			
		}
	}
    public static void dispose(){
        Registry.instance().callExtensions("com.venky.swf.db.Database.beforeClose");
        for (String pool : tablesInPool.keySet()){
            tablesInPool.get(pool).clear();
        }
        tablesInPool.clear();
        for (String key : configQueryCacheMap.keySet()){
            configQueryCacheMap.get(key).clear();
        }
        configQueryCacheMap.clear();
        ConnectionManager.instance().close();
        ModelReflector.dispose();
        TableReflector.dispose();
    }

    public static String getCaller(){
        StackTraceElement[] e = new Exception().getStackTrace();
        for (StackTraceElement elem : e) {
            if (elem.getClassName().startsWith("com.venky.swf.db") || elem.getClassName().startsWith("com.venky.swf.sql") || elem.getClassName().startsWith("sun.") || elem.getClassName().startsWith("java.")) {
                continue;
            }
            return elem.toString();
        }

        StringWriter w = new StringWriter();
        new Exception().printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    public void resetIdGeneration(){
        for (String pool : ConnectionManager.instance().getPools()){
        	JdbcTypeHelper helper = getJdbcTypeHelper(pool);
            for (Table<? extends Model> table : Database.getTables(pool).values()){
                if (table.isReal() && table.isExistingInDatabase()){
                   helper.updateSequence(table);
                }
            }
        }
    }

    int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    public int getTransactionIsolationLevel(){
    	return transactionIsolationLevel;
    }
	public void setTransactionIsolationLevel(int newIsolationLevel) {
		if (transactionIsolationLevel == newIsolationLevel) {
			return;
		}
		if (isActiveTransactionPresent()){
			throw new RuntimeException("Cannot Set Isolation Level when a Transaction is going on");
		}
		transactionIsolationLevel = newIsolationLevel;
		Config.instance().getLogger(getClass().getName()).info ("Set transaction isolation level to " + newIsolationLevel);
	}
	public void resetTransactionIsolationLevel(){
		setTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
	}
  	

}
