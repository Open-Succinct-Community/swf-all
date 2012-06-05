/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.User;
import com.venky.swf.db.model.reflection.ModelReflector;
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
					Logger.getLogger(Database.class.getName()).warning("Not all Transactions in the application has a finally rollback block! Any way. Recovering...");
					transactionStack.clear();
				}
				connection.rollback();
				connection.close();
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

	public QueryCache getCache(ModelReflector ref) {
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
		
		private void rollback(Savepoint sp){
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
		}

		public void commit() {
			releaseSavepoint(savepoint);
            savepoint = setSavepoint(String.valueOf(transactionStack.size()));
			updateTransactionStack();
			if (!transactionStack.isEmpty()){
				Transaction parent = transactionStack.peek();
				parent.merge(this);
			}
		}

		public void rollback() {
			rollback(savepoint);
			updateTransactionStack();
		}

		private void updateTransactionStack() {
			Transaction completedTransaction = transactionStack.peek();
			if (completedTransaction != this) {
				throw ex;
			}
			transactionStack.pop();
			
			if (transactionStack.isEmpty()) {
				try {
					Registry.instance().callExtensions("before.commit",this);
					getConnection().commit();
					Registry.instance().callExtensions("after.commit",this);
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
		}

        private void merge(Transaction completedTransaction) {
        	for (String table : completedTransaction.txnQueryCacheMap.keySet()){
    			QueryCache completedTransactionCache = completedTransaction.txnQueryCacheMap.get(table);
        		if (txnQueryCacheMap.containsKey(table)){
        			QueryCache currentTransactionCache = txnQueryCacheMap.get(table);
        			currentTransactionCache.merge(completedTransactionCache);
        		}else {
        			txnQueryCacheMap.put(table, completedTransactionCache);
        		}
        	}
        	
		}
		public PreparedStatement createStatement(String sql) throws SQLException{
            return getConnection().prepareStatement(sql);
		}
        public PreparedStatement createStatement(String sql,String[] columnNames) throws SQLException{ 
			return getConnection().prepareStatement(sql, columnNames );
		}

		private Map<String, QueryCache> txnQueryCacheMap = new HashMap<String, QueryCache>();

		public QueryCache getCache(ModelReflector ref) {
			String tableName = ref.getTableName();
			
        	QueryCache queryCache = txnQueryCacheMap.get(tableName);
        	if (queryCache != null){
        		return queryCache;
        	}
        	
        	QueryCache parentQueryCache = null ;
            if (transactionNo >= 1){
            	Transaction parentTransaction = transactionStack.get(transactionNo - 1);//Parent transaction
        		parentQueryCache = parentTransaction.txnQueryCacheMap.get(tableName);
    		}

            if (parentQueryCache != null){
    			queryCache = parentQueryCache.copy();
    		}else {
    			queryCache = new QueryCache(tableName);
            }
			txnQueryCacheMap.put(tableName, queryCache);
			return queryCache;
		}

	}

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
				dbModified = table.sync();
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

	private static void ensureFactorySettings() {
    	List<String> installerNames = Config.instance().getInstallers();
		try {
			for (String installerName : installerNames){
				Logger.getLogger(Database.class.getName()).info("Installing ... " + installerName );
				try{
					Installer installer = (Installer)Class.forName(installerName).newInstance();
					installer.install();
				}finally {
					Logger.getLogger(Database.class.getName()).info("done!");
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
				Properties info = new Properties();
				String dbURL = System.getenv("DATABASE_URL");
		    	
		    	if (!ObjectUtil.isVoid(dbURL)){
		    		Logger.getLogger(Database.class.getName()).fine("DATABASE_URL:" + dbURL );
		    		URI uri;
					try {
						uri = new URI(dbURL);
					} catch (URISyntaxException e) {
						throw new RuntimeException(e);
					}
					String jdbcurl = null; 
		    		if (uri.getScheme().equals("postgres")){
		    			jdbcurl = "jdbc:postgresql://";	
		    		}else if (uri.getScheme().equals("mysql")){
		    			jdbcurl = "jdbc:mysql://";
		    		}else if (uri.getScheme().equals("derby")){
		    			jdbcurl = "jdbc:derby://";
		    		}
		    		jdbcurl = jdbcurl + uri.getHost() + uri.getPath() ;
					info.setProperty("url", jdbcurl);
					info.setProperty("username", uri.getUserInfo().split(":")[0]);
					info.setProperty("password", uri.getUserInfo().split(":")[1]);
		    	}else { 
					info.setProperty("url", Config.instance().getProperty("swf.jdbc.url"));
					info.setProperty("username", Config.instance().getProperty("swf.jdbc.userid"));
					info.setProperty("password",Config.instance().getProperty("swf.jdbc.password"));
		    	}
				
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
			ensureFactorySettings();
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
