/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;

import com.venky.core.checkpoint.Checkpoint;
import com.venky.core.checkpoint.Checkpointed;
import com.venky.core.checkpoint.MergeableMap;
import com.venky.core.collections.IgnoreCaseMap;
import com.venky.extension.Registry;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.model.reflection.TableReflector;
import com.venky.swf.db.platform.Platform;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Table;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
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
		if (getConnection() == null) {
			throw new RuntimeException("Failed to open connection to database");
		}
		this.currentUser = (User)currentUser;
	}
	
	public User getCurrentUser(){
		return currentUser;
	}
	
	public void close() {
		closeConnection();
		currentUser = null;
	}
	
	public static void dispose(){
		TaskManager.instance().shutdown();
		tables.clear();
		for (String key : configQueryCacheMap.keySet()){
			configQueryCacheMap.get(key).clear();
		}
		configQueryCacheMap.clear();
		_ds = null; 
		_helper = null;
		
		ModelReflector.dispose();
		TableReflector.dispose();
		LuceneIndexer.dispose();
	}

	private Connection connection = null;
	private Connection getConnection() {
		if (connection == null) {
			try {
				connection = createConnection();
			} catch (SQLException ex) {
				throw new RuntimeException(ex);
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		return connection;
	}

	private void closeConnection() {
		if (connection != null) {
			try {
				if (!transactionStack.isEmpty()){
					Config.instance().getLogger(Database.class.getName()).warning("Not all Transactions in the application has a finally rollback block! Any way. Recovering...");
					transactionStack.clear();
				}
				txnUserAttributes.rollback(); //All check points are clear.
				txnUserAttributes.getCurrentValue().clear(); // Now restore the initial value to a clear map.
				if (!connection.isClosed()){
					connection.rollback();
					connection.close();
				}
			} catch (SQLException ex) {
				throw new RuntimeException(ex);
			} finally {
				connection = null;
			}
		}
	}



	private Stack<Transaction> transactionStack = new Stack<Transaction>();
	public Transaction createTransaction() {
		Transaction transaction = new Transaction();
		transactionStack.push(transaction);
		return transaction;
	}

	public Transaction getCurrentTransaction(){
		if (transactionStack.isEmpty()) {
			createTransaction();
		}
		return transactionStack.peek();
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
			return getCurrentTransaction().getCache(ref);
		}
	}

	public class Transaction implements _ITransaction{
		private Savepoint savepoint = null;
		private RuntimeException ex = null;
		private int transactionNo = -1 ;
		private Checkpoint<MergeableMap<String,Object>> checkpoint = null;

		private Savepoint setSavepoint(String name){
			try {
				return getConnection().setSavepoint(name);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			
		}
		private void releaseSavepoint(Savepoint sp){
			try {
				getConnection().releaseSavepoint(sp);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		private void rollbackToSavePoint(Savepoint sp){
			try {
				getConnection().rollback(sp);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Transaction() {
			transactionNo = transactionStack.size();
            ex = new RuntimeException("Transaction " + transactionNo + " not completed ");
            savepoint = setSavepoint(String.valueOf(transactionNo));
            checkpoint = txnUserAttributes.createCheckpoint();
		}

		public void commit() {
			releaseSavepoint(savepoint);
            savepoint = setSavepoint(String.valueOf(transactionStack.size()));
			txnUserAttributes.commit(checkpoint);
            updateTransactionStack();
			if (transactionStack.isEmpty()){
				try {
					Registry.instance().callExtensions("before.commit",this);
					getConnection().commit();
					registerLockRelease();
					Registry.instance().callExtensions("after.commit",this);
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		}
		private void registerLockRelease(){
			for (QueryCache cache: configQueryCacheMap.values()){
				cache.registerLockRelease();
			}
		}
		public void rollback(Throwable th) {
			boolean entireTransactionIsRolledBack = getJdbcTypeHelper().hasTransactionRolledBack(th); 
			if (!entireTransactionIsRolledBack){
				rollbackToSavePoint(savepoint);
			}
			txnUserAttributes.rollback(checkpoint);
			updateTransactionStack();
			if (transactionStack.isEmpty()){
				try {
					getConnection().rollback();
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
		/*
		public void rollback() {
			rollback(null);
		}*/

		private void updateTransactionStack() {
			Transaction completedTransaction = transactionStack.peek();
			if (completedTransaction != this) {
				throw ex;
			}
			transactionStack.pop();
		}

		public PreparedStatement createStatement(String sql) throws SQLException{
            return getConnection().prepareStatement(sql);
		}
        public PreparedStatement createStatement(String sql,String[] columnNames) throws SQLException{ 
        	return getConnection().prepareStatement(sql, columnNames );
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
				Config.instance().getLogger(getClass().getName()).warning(value.getClass().getName() + " not Serializable or Cloneable. Checkpointing in nested transactions may exhibit unexpected behaviour!");
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
			Set<String> models = Database.getInstance().getCurrentTransaction().getAttribute("tables.modified");
	    	if (models == null){
	    		models = new HashSet<String>();
	    		Database.getInstance().getCurrentTransaction().setAttribute("tables.modified", models);
	    	}
	    	return models;
		}
	}

	private Checkpointed<MergeableMap<String,Object>> txnUserAttributes = new Checkpointed<MergeableMap<String,Object>>(new MergeableMap<String, Object>());
	
	

	// Class level methods and variables.
	private static Map<String, QueryCache> configQueryCacheMap = new HashMap<String, QueryCache>();

	private static Map<String, Table<?>> tables = new IgnoreCaseMap<Table<?>>();
	public static Map<String, Table<?>> getTables() {
		return tables;
	}
	@SuppressWarnings("unchecked")
	public static <M extends Model> Table<M> getTable(Class<M> modelClass) {
		return (Table<M>) getTable(Table.tableName(modelClass));
	}

	@SuppressWarnings("unchecked")
	public static <M extends Model> Table<M> getTable(String tableName) {
		return (Table<M>) tables.get(tableName);
	}

	public static Set<String> getTableNames() {
		return tables.keySet();
	}

	public static void migrateTables() {
		boolean dbModified = false;
		loadTables(dbModified);
		for (Table<?> table : tables.values()) {
			if (table.isVirtual()) {
				continue;
			}
			if (!table.isExistingInDatabase()) {
				table.createTable();
				dbModified = true;
			} else if (table.getModelClass() == null) {
				table.dropTable();
				dbModified = true;
			} else {
				dbModified = table.sync() || dbModified;
			}
		}
		loadTables(dbModified);
	}
	public static void loadTables(boolean reload) {
		if (reload) {
			tables.clear();
		}
		if (!tables.isEmpty()) {
			return;
		}
		loadTablesFromModel();
		loadTablesFromDB();
	}

	@SuppressWarnings("rawtypes")
	private static void loadTablesFromDB() {
		try { 
			Connection conn =  getInstance().getConnection();
			DatabaseMetaData meta = conn.getMetaData();
            ResultSet tablesResultSet = meta.getTables(null, getSchema(), null, new String[]{"TABLE"});
			while (tablesResultSet.next()) {
				String tableName = tablesResultSet.getString("TABLE_NAME");
				Table table = tables.get(tableName);
				if (table == null){
					table = new Table(tableName);
					tables.put(tableName, table);
				}
				table.setExistingInDatabase(true);
                ResultSet columnResultSet = meta.getColumns(null,getSchema(), tableName, null);
				while (columnResultSet.next()) {
                    String columnName  = columnResultSet.getString("COLUMN_NAME");
                    table.getColumnDescriptor(columnName,true).load(columnResultSet);
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadTablesFromModel() {
		List<String> modelClasses = Config.instance().getModelClasses();

		for (String className : modelClasses) {
			try {
				Class<?> modelClass = Class.forName(className);
                if (!className.equals(Model.class.getName()) && modelClass.isInterface() && Model.class.isAssignableFrom(modelClass)){
					Table table = new Table(modelClass);
					table.setExistingInDatabase(false);
					String tableName = table.getTableName(); 
					if (table.getRealTableName() != null && !tables.containsKey(tableName)) {
						tables.put(table.getTableName(), table);
					}
				}
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
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

	private static BasicDataSource _ds = null;
	private static BasicDataSource getDataSource() {
		if (_ds == null){
			synchronized (Database.class) {
				Properties info = Platform.getConnectionProperties();
				
		    	String driver = Config.instance().getProperty("swf.jdbc.driver");
				info.setProperty("driverClassName",driver);

				try {
					Class<?> driverClass = Class.forName(driver);
					_ds = (BasicDataSource)BasicDataSourceFactory.createDataSource(info);
					_helper = JdbcTypeHelper.instance(driverClass);
				}catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return _ds;
	}

	public static Database getInstance() {
		return getInstance(false);
	}

	private static ThreadLocal<Database> _instance = new ThreadLocal<Database>();
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

	private static JdbcTypeHelper _helper = null;
	public static JdbcTypeHelper getJdbcTypeHelper() {
		return _helper;
	}
	
	private static String getSchema() {
		return Config.instance().getProperty("swf.jdbc.dbschema");
	}

	private static boolean isSchemaToBeSetOnConnection() {
		return Boolean.valueOf(Config.instance().getProperty("swf.jdbc.dbschema.setonconnection"));
	}
	
    private static Connection createConnection() throws SQLException, ClassNotFoundException{
		Connection conn = getDataSource().getConnection();
		conn.setAutoCommit(false);
		if (isSchemaToBeSetOnConnection()) {
			String schemaSettingCommand = Config.instance().getProperty("swf.jdbc.set.dbschema.command", "set schema ?");
			PreparedStatement stmt = conn.prepareStatement(schemaSettingCommand);
			if (schemaSettingCommand.indexOf('?') >= 0) {
				stmt.setString(1, getSchema());
			}
			stmt.executeUpdate();
			conn.commit();
		}
		if (conn.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED)) {
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		}

		return conn;
	}

	public static void shutdown() {
		try {
			getDataSource().close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		String jdbcurl = Config.instance().getProperty("swf.jdbc.url");
		String driver = Config.instance().getProperty("swf.jdbc.driver");
		if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
			try {
				jdbcurl = jdbcurl + ";shutdown=true";
                String userid = Config.instance().getProperty("swf.jdbc.userid");
                String password = Config.instance().getProperty("swf.jdbc.password");
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
