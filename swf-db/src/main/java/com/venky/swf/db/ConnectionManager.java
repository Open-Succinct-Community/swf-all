package com.venky.swf.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.platform.Platform;
import com.venky.swf.routing.Config;

public class ConnectionManager {
	private static ConnectionManager _instance ; 
	public static ConnectionManager instance(){
		if (_instance != null){
			return _instance;
		}
		synchronized (ConnectionManager.class) {
			if (_instance == null){
				_instance = new ConnectionManager();
			}
		}
		return _instance;
	}
	private ConnectionManager(){
	
	}

	private Class<?> driverClass = null;
	public Class<?> getDriverClass(){ 
		if (driverClass != null){
			return driverClass;
		}
		synchronized (this) {
			if (driverClass == null){
				try {
			    	String driver = Config.instance().getProperty("swf.jdbc.driver");
					driverClass = Class.forName(driver);
				}catch (Exception e){
					throw new RuntimeException(e);
				}
			}
		}
		return driverClass;
	}
	
	public JdbcTypeHelper getJdbcTypeHelper() {
		return JdbcTypeHelper.instance(getDriverClass());
	}
	
	private DataSource ds;
	public DataSource getDataSource(){
		if (!isConnectionPooled()){
			return null;
		}
		synchronized (this) {
			if (ds == null){
				Properties info = Platform.getConnectionProperties();
				info.setProperty("driverClassName",getDriverClass().getName());
				info.setProperty("validationQuery",Config.instance().getProperty("swf.jdbc.validationQuery", "select 1 as dbcp_connection_test"));
				info.setProperty("testOnBorrow", "true");
				info.setProperty("testOnReturn", "true");
				info.setProperty("testWhileIdle", "true");
				info.setProperty("timeBetweenEvictionRunsMillis",String.valueOf(1000*60*2));
				info.setProperty("minEvictableIdleTimeMillis",String.valueOf(1000*60));
				try {
					Class<?> c = Class.forName("org.apache.commons.dbcp.BasicDataSourceFactory");
					Method m = c.getMethod("createDataSource", Properties.class);
					ds = (DataSource) m.invoke(c, info);
				}catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return ds;
	}
	public boolean isConnectionPooled(){
		String value = Config.instance().getProperty("swf.jdbc.connection.pooling", "true");
		return Boolean.valueOf(StringUtil.valueOf(Database.getJdbcTypeHelper().getTypeRef(Boolean.class).getTypeConverter().valueOf(value)));
	}
	
	public Connection getConnection(){
		DataSource ds = getDataSource(); 
		try {
			if (ds == null){
				Properties props = Platform.getConnectionProperties();
				return DriverManager.getConnection(props.getProperty("url"), Platform.getConnectionProperties());
			}else {
				return ds.getConnection();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	public void close() {
		DataSource ds = getDataSource();
		if (ds != null){
			try {
				Method close = ds.getClass().getMethod("close");
				close.invoke(ds);
				Config.instance().getLogger(getClass().getName()).info("Closed Data Source");
			} catch (Exception e) {
				Config.instance().getLogger(getClass().getName()).info("DataSource not closed " + e.getMessage());
			}finally {
        ds = null;
      }
		}
	}
	
}
