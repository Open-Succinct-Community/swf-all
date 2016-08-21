/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.TimeZone;

import com.venky.core.date.DateUtils;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;

/**
 *
 * @author venky
 */
public class SQLDroidHelper extends JdbcTypeHelper{

	@Override
	public boolean isSavepointManagedByJdbc(){
    	return false;
    }
	
	@Override
	public String getEstablishSavepointStatement(String name){
		return "; SAVEPOINT S" + name ;
	}

	@Override
	public String getReleaseSavepointStatement(String name){
		return "; RELEASE S" + name ;
	}

	@Override
	public String getRollbackToSavepointStatement(String name){
		return "; ROLLBACK TO S" + name ;
	}

	@Override
    public String getAutoIncrementInstruction() {
            return (" INTEGER ");
    }
	
    @Override
    public String getCurrentTimeStampKW(){
    	return "'1970-01-01 00:00:00.000'";
    }
    @Override
    public String getCurrentDateKW(){
    	return "'1970-01-01'";
    }

    public String getForUpdateLiteral(){
    	return "";
    }
    public boolean isQueryTimeoutSupported(){ 
		return false;
	}
    @Override
	public String getDefaultKW(TypeRef<?> ref, Object value) {
    	if (value instanceof Boolean){
    		if ((Boolean)value){
    			return "1";
    		}else {
    			return "0";
    		}
    	}
    	return super.getDefaultKW(ref, value);
	}
    @Override
    public void setBinaryStream(PreparedStatement st, int i, ByteArrayInputStream in) throws SQLException {
		st.setBytes(i, StringUtil.readBytes(in));
	}


    protected SQLDroidHelper() {
            /**
             * Specify size and scale for a data type only if the database accepts
             * them during table creation
             */
            registerjdbcSQLType(Boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIGINT, "INTEGER", 0, 0, false,false,
                            new BooleanConverter()));
            registerjdbcSQLType(boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIGINT, "INTEGER", 0, 0, false,false,
                            new BooleanConverter()));

            registerjdbcSQLType(Byte.class, new TypeRef<Byte>(java.sql.Types.BIGINT,
                            "INTEGER", 0, 0, false,false,new ByteConverter()));

            registerjdbcSQLType(byte.class, new TypeRef<Byte>(java.sql.Types.BIGINT,
                            "INTEGER", 0, 0, false,false,new ByteConverter()));

            registerjdbcSQLType(Short.class,
                            new TypeRef<Short>(java.sql.Types.BIGINT, "INTEGER", 0, 0,false,false,
                                            new ShortConverter()));
            registerjdbcSQLType(short.class,
                            new TypeRef<Short>(java.sql.Types.BIGINT, "INTEGER", 0, 0,false,false,
                                            new ShortConverter()));

            registerjdbcSQLType(Integer.class,
                            new TypeRef<Integer>(java.sql.Types.BIGINT, "INTEGER", 0, 0,false,false,
                                            new IntegerConverter()));

            registerjdbcSQLType(int.class, new TypeRef<Integer>(java.sql.Types.BIGINT, 
                            "INTEGER", 0, 0,false,false, new IntegerConverter()));
            
            registerjdbcSQLType(Long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "INTEGER", 0, 0, false,false,new LongConverter()));
            
            registerjdbcSQLType(long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "INTEGER", 0, 0, false,false, new LongConverter()));

            
            registerjdbcSQLType(BigDecimal.class, new TypeRef<BigDecimal>(
                            java.sql.Types.VARCHAR, "TEXT", 0, 0, false,false,
                            new BigDecimalConverter()));// also NUMERIC

            registerjdbcSQLType(Float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, false,false,new FloatConverter()));
            registerjdbcSQLType(float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, false,false,new FloatConverter()));

            registerjdbcSQLType(Double.class, new TypeRef<Double>(
                            java.sql.Types.REAL, "REAL", 0, 0,false,false, new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT
            registerjdbcSQLType(double.class, new TypeRef<Double>(
                            java.sql.Types.REAL, "REAL", 0, 0, false,false,new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT
            registerjdbcSQLType(Bucket.class, new TypeRef<Bucket>(
                    java.sql.Types.REAL, "REAL", 0, 0,false,false, new BucketConverter())); // ALSO

            registerjdbcSQLType(Date.class, new TypeRef<Date>(java.sql.Types.VARCHAR,
                            "TEXT", 0, 0, true,false, new DateConverter(DateUtils.ISO_DATE_FORMAT_STR,TimeZone.getDefault())));
            
            registerjdbcSQLType(Time.class, new TypeRef<Time>(
                            java.sql.Types.VARCHAR, "TEXT", 0, 0, true ,false, new TimeConverter(DateUtils.ISO_TIME_FORMAT_STR,TimeZone.getDefault())));
            
            registerjdbcSQLType(java.sql.Timestamp.class, new TypeRef<Timestamp>(
                            java.sql.Types.VARCHAR, "TEXT", 0, 0, true,false,
                            new TimestampConverter(DateUtils.ISO_DATE_TIME_FORMAT_STR,TimeZone.getDefault())));

            registerjdbcSQLType(String.class, new TypeRef<String>(
                            java.sql.Types.VARCHAR, "TEXT", 0, 0, true,true,
                            new StringConverter())); // ALSO CHAR, LONG VARCHAR

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.VARCHAR,
                            "TEXT", 0, 0, true , true,new ReaderConverter()));

            registerjdbcSQLType(InputStream.class, new TypeRef<InputStream>(java.sql.Types.BLOB,
                            "BLOB", 0, 0, true , true,new InputStreamConverter()));

    }
}
