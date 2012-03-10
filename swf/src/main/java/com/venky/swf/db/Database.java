/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.util.PackageUtil;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.db.table.QueryCache;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;


/**
 *
 * @author venky
 */
public class Database {

    private Database() {
        
    }
    
    private static ThreadLocal<Database>  _instance = new ThreadLocal<Database>();
    public static Database getInstance(){
    	return getInstance(false);
    }
    public static Database getInstance(boolean migrate){
        if (_instance.get() == null){
            Database db= new Database();
            _instance.set(db);
            if (migrate){
            	db.migrateTables();
            }else {
            	db.loadTables(false);
            }
        }
        return _instance.get();
    }
    
    private String getSchema(){
        return Config.instance().getProperty("swf.jdbc.dbschema");
    }
    private boolean isSchemaToBeSetOnConnection(){
    	return Boolean.valueOf(Config.instance().getProperty("swf.jdbc.dbschema.setonconnection"));
    }
    private Connection createConnection() throws SQLException, ClassNotFoundException{
        String jdbcurl = Config.instance().getProperty("swf.jdbc.url");
        String userid = Config.instance().getProperty("swf.jdbc.userid");
        String password = Config.instance().getProperty("swf.jdbc.password");
        String schema = getSchema();
        String driver = Config.instance().getProperty("swf.jdbc.driver");
        Class<?> driverClass = Class.forName(driver);
        helper = JdbcTypeHelper.instance(driverClass);
        Properties info = new Properties();
        info.setProperty("user", userid);
        info.setProperty("password", password);
        
        
        Connection conn  = DriverManager.getConnection(jdbcurl,info);
        conn.setAutoCommit(false);
        if (isSchemaToBeSetOnConnection()){
            PreparedStatement stmt = conn.prepareStatement("set schema ?");
            stmt.setString(1, schema);
            stmt.executeUpdate();
            conn.commit();
        }
        
        
        
        return conn;
    }

    private JdbcTypeHelper helper = null ;
    
    public JdbcTypeHelper getJdbcTypeHelper(){
        return helper;
    }
    
    
    public void shutdown() {
        String jdbcurl = Config.instance().getProperty("swf.jdbc.url");
        String driver = Config.instance().getProperty("swf.jdbc.driver");
        if (driver.contains("derby")){
            try {
                jdbcurl = jdbcurl + ";shutdown=true";
                String userid = Config.instance().getProperty("swf.jdbc.userid");
                String password = Config.instance().getProperty("swf.jdbc.password");
                Properties info = new Properties();
                info.setProperty("user", userid);
                info.setProperty("password", password);
                DriverManager.getConnection(jdbcurl,info);
            } catch (SQLException ex) {
                if (ex.getSQLState().equals("08006") && ex.getErrorCode() == 45000){
                    System.out.println("Derby db closed!");
                }else {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    
    private Connection connection = null; 
    public void open(){
        assert (getConnection() != null);
        assert connection != null;
    }
    
    public void close(){
        closeConnection();
        assert connection == null;
    }
    public Connection getConnection(){ 
        if (connection == null){
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
    
    private void closeConnection(){
        if (connection != null){
            try {
                connection.rollback();
                connection.close();
            }catch (SQLException ex){
                throw new RuntimeException(ex);
            }finally{
                connection = null;
            }
        }
    }
    
    
    private Map<String,Table<?>> tables = new IgnoreCaseMap<Table<?>>();
    
    public Map<String, Table<?>> getTables() {
		return tables;
	}

	@SuppressWarnings("unchecked")
	public <M extends Model> Table<M> getTable(Class<M> modelClass){
        return (Table<M>)getTable(Table.tableName(modelClass));
    }
    @SuppressWarnings("unchecked")
	public <M extends Model> Table<M> getTable(String tableName){
        return (Table<M>)tables.get(tableName);
    }
    public Set<String> getTableNames(){ 
        return tables.keySet();
    }

    public void migrateTables() {
        boolean dbModified = false;
        loadTables(dbModified);
        for (Table<?> table: tables.values()){
            if (!table.isExistingInDatabase()){
            	if (table.isReal()){
                    table.createTable();
                    dbModified = true;
            	}
            }else if (table.getModelClass() == null){
                table.dropTable();
                dbModified = true;
            }else {
                dbModified = table.sync();
            }
        }
        loadTables(dbModified);
        ensureFactorySettings();
    }
    private void ensureFactorySettings(){
    	String installerName = Config.instance().getProperty("swf.default.configuration.installer");
    	try {
			Installer installer = (Installer)Class.forName(installerName).newInstance();
			installer.install();
			Database.getInstance().getCurrentTransaction().commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	
    }
    public void loadTables(boolean reload){
        if (reload){
            tables.clear();
        }
        if (!tables.isEmpty()){
            return;
        }
        loadTablesFromModel();
        loadTablesFromDB();
    }
    
    private void loadTablesFromDB(){
        try {
            DatabaseMetaData meta = getConnection().getMetaData();
            ResultSet tablesResultSet = meta.getTables(null, getSchema(), null, null);
            while (tablesResultSet.next()){
                String tableName = tablesResultSet.getString("TABLE_NAME");
                Table table = new Table(tableName);
                table.setExistingInDatabase(true);
                tables.put(table.getTableName(),table); // Override from db.
                ResultSet columnResultSet = meta.getColumns(null,getSchema(), tableName, null);
                while(columnResultSet.next()){
                    String columnName  = columnResultSet.getString("COLUMN_NAME");
                    table.getColumnDescriptor(columnName,true).load(columnResultSet);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void loadTablesFromModel(){ 
        List<String> modelClasses = new ArrayList<String>();
        
        for (String root : Config.instance().getModelPackageRoots()){
        	for (URL url:Config.instance().getResouceBaseUrls()){
        		modelClasses.addAll(PackageUtil.getClasses(url, root.replace('.', '/')));
        	}
        }
        
        for (String className : modelClasses){
            try {
                Class<?> modelClass = Class.forName(className);
                if (!className.equals(Model.class.getName()) && modelClass.isInterface() && Model.class.isAssignableFrom(modelClass)){
                    Table table = new Table(modelClass);
                    table.setExistingInDatabase(false);
                    if (!tables.containsKey(table.getTableName())) {
                        tables.put(table.getTableName(), table);
                    }
                }
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    private Stack<Transaction> transactionStack = new Stack<Transaction>();
    public Transaction createTransaction() throws SQLException{ 
        Transaction transaction = new Transaction();
        transactionStack.push(transaction);
        return transaction;
    }
    
    public Transaction getCurrentTransaction() throws SQLException{ 
        if (transactionStack.isEmpty()){
            createTransaction();
        }
        return transactionStack.peek();
    }

    
    private static Map<Class<? extends Model>,QueryCache<? extends Model>> configQueryCacheMap = new HashMap<Class<? extends Model>, QueryCache<? extends Model>>();    
    public <M extends Model> QueryCache<M> getCache(Class<M> modelClass) throws SQLException{
    	if (ModelReflector.instance(modelClass).isAnnotationPresent(CONFIGURATION.class)){
    		QueryCache<M> cacheEntry = (QueryCache<M>) configQueryCacheMap.get(modelClass);
    		if (cacheEntry == null){
    			synchronized (configQueryCacheMap) {
    				cacheEntry = (QueryCache<M>) configQueryCacheMap.get(modelClass);
    				if (cacheEntry == null){
	    				cacheEntry = new QueryCache<M>(modelClass);
	    				configQueryCacheMap.put(modelClass,cacheEntry);
    				}
				}
    		}
    		return cacheEntry;
    	}else {
    		return getCurrentTransaction().getCache(modelClass);
    	}
    }
    
    public class Transaction{
        private Savepoint savepoint = null;
        private RuntimeException ex = null; 
        
        public Transaction() throws SQLException{
            int transactionNo = transactionStack.size();
            ex = new RuntimeException("Transaction " + transactionNo + " not completed ");
            savepoint = getConnection().setSavepoint(String.valueOf(transactionNo));
        }
        
        public void commit() throws SQLException{ 
            getConnection().releaseSavepoint(savepoint);
            savepoint = getConnection().setSavepoint(String.valueOf(transactionStack.size()));
            updateTransactionStack();
        }
        
        public void rollback() throws SQLException {
            getConnection().rollback(savepoint);
            updateTransactionStack();
        }
        
        private void updateTransactionStack() throws SQLException{
            Transaction t = transactionStack.peek();
            if (t != this){ 
                throw ex;
            }
            transactionStack.pop();
            if (transactionStack.isEmpty()){
                getConnection().commit();
            }
        }
        
        public PreparedStatement createStatement(String sql) throws SQLException{ 
            return getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
        public PreparedStatement createStatement(String sql,String[] columnNames) throws SQLException{ 
            return getConnection().prepareStatement(sql, columnNames);
        }
        
        private Map<Class<? extends Model>,QueryCache<? extends Model>> txnQueryCacheMap = new HashMap<Class<? extends Model>, QueryCache<? extends Model>>();
        
        public <M extends Model> QueryCache<M> getCache(Class<M> modelClass){
        	QueryCache<M> queryCache = (QueryCache<M>) txnQueryCacheMap.get(modelClass);
        	if (queryCache == null){
	        	queryCache = new QueryCache<M>(modelClass);
	        	txnQueryCacheMap.put(modelClass, queryCache);
        	}
        	return queryCache;
        }
    
    
    }

}
