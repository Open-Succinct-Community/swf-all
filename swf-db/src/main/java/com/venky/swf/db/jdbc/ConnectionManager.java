package com.venky.swf.db.jdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import com.venky.cache.Cache;
import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper;
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
	    loadPools();
	}

	private Cache<String,Class<?>> driverClassCache = new Cache<String, Class<?>>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3352757744534631289L;

		@Override
		protected Class<?> getValue(String pool) {
			try {
				String driver = Config.instance().getProperty(getNormalizedPropertyName("swf.jdbc."+pool+".driver"));
				return Class.forName(driver);
			}catch (Exception e){
				throw new RuntimeException(e);
			}
		}
	};
	public String getNormalizedPropertyName(String name){
		return name.replace("..",".");
	}

    private List<String> pools = null;
    private void loadPools(){
        if (pools == null) {
            pools = new ArrayList<String>();
            List<String> keys = Config.instance().getPropertyKeys("swf\\.jdbc\\..*driver");
            for (String key : keys) {
                StringTokenizer t = new StringTokenizer(key, ".");
                String pool = "";
                if (t.countTokens() > 3) {
                    t.nextToken();//swf
                    t.nextToken();//jdbc
                    pool = t.nextToken();//poolname
                }
                pools.add(pool);
            }
        }
    }
	public List<String> getPools(){
        return pools;
	}
	public Class<?> getDriverClass(String pool){
		return driverClassCache.get(pool);
	}
	
	public JdbcTypeHelper getJdbcTypeHelper(String pool) {
		return JdbcTypeHelper.instance(getDriverClass(pool));
	}
	
	private Cache<String,DataSource> dsCache = new Cache<String, DataSource>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4782855310119130688L;
		@Override
        protected DataSource getValue(String pool) {
            if (!isConnectionPooled(pool)){
                return null;
            }
            Properties info = Platform.getConnectionProperties(pool);
            info.setProperty("driverClassName",getDriverClass(pool).getName());
            info.setProperty("validationQuery",Config.instance().getProperty(getNormalizedPropertyName("swf.jdbc."+pool+".validationQuery"), "select 1 as dbcp_connection_test"));
            info.setProperty("testOnBorrow", "true");
            info.setProperty("testOnReturn", "true");
            info.setProperty("testWhileIdle", "true");
            info.setProperty("timeBetweenEvictionRunsMillis",String.valueOf(1000*60*2));
            info.setProperty("minEvictableIdleTimeMillis",String.valueOf(1000*60));
            try {
                Class<?> c = Class.forName("org.apache.commons.dbcp.BasicDataSourceFactory");
                Method m = c.getMethod("createDataSource", Properties.class);
                return (DataSource) m.invoke(c, info);
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public boolean isConnectionPooled(String pool){
            String value = Config.instance().getProperty(getNormalizedPropertyName("swf.jdbc."+pool+".connection.pooling"), "true");
            return Boolean.valueOf(StringUtil.valueOf(Database.getJdbcTypeHelper(pool).getTypeRef(Boolean.class).getTypeConverter().valueOf(value)));
        }
    };



	public Connection createConnection(String pool){
		DataSource ds = dsCache.get(pool);
		try {
			if (ds == null){
				Properties props = Platform.getConnectionProperties(pool);
				return DriverManager.getConnection(props.getProperty("url"), Platform.getConnectionProperties(pool));
			}else {
				return ds.getConnection();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	public boolean isPoolReadOnly(String pool){
		return Config.instance().getBooleanProperty(getNormalizedPropertyName("swf.jdbc."+pool+".readOnly"),false);
	}
	public void close() {
        List<String> allPools = new ArrayList<String>(dsCache.keySet());
        for (String pool : allPools) {
            DataSource ds = dsCache.get(pool);
            if (ds != null) {
                try {
                    Method close = ds.getClass().getMethod("close");
                    close.invoke(ds);
                    Config.instance().getLogger(getClass().getName()).info("Closed Data Source");
                } catch (Exception e) {
                    Config.instance().getLogger(getClass().getName()).info("DataSource not closed " + e.getMessage());
                } finally {
                    dsCache.remove(pool);
                }
            }
        }
    }
}
