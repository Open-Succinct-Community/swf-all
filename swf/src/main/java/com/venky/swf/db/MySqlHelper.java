/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 *
 * @author venky
 */
public class MySqlHelper extends JdbcTypeHelper{
	
    @Override
    public String getAutoIncrementInstruction() {
            return (" INTEGER NOT NULL AUTO_INCREMENT ");
    }
    protected MySqlHelper() {
            /**
             * Specify size and scale for a data type only if the database accepts
             * them during table creation
             */
            registerjdbcSQLType(Boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BIT", 0, 0,
                            new BooleanConverter()));
            registerjdbcSQLType(boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BIT", 0, 0,
                            new BooleanConverter()));

            registerjdbcSQLType(Byte.class, new TypeRef<Byte>(java.sql.Types.TINYINT,
                            "TINYINT", 0, 0, new ByteConverter()));

            registerjdbcSQLType(byte.class, new TypeRef<Byte>(java.sql.Types.TINYINT,
                            "TINYINT", 0, 0, new ByteConverter()));

            registerjdbcSQLType(Short.class,
                            new TypeRef<Short>(java.sql.Types.SMALLINT, "SMALLINT", 0, 0,
                                            new ShortConverter()));
            registerjdbcSQLType(short.class,
                            new TypeRef<Short>(java.sql.Types.SMALLINT, "SMALLINT", 0, 0,
                                            new ShortConverter()));

            registerjdbcSQLType(Integer.class,
                            new TypeRef<Integer>(java.sql.Types.INTEGER, "INTEGER", 0, 0,
                                            new IntegerConverter()));

            registerjdbcSQLType(int.class, new TypeRef<Integer>(java.sql.Types.INTEGER,
                            "INTEGER", 0, 0, new IntegerConverter()));

            registerjdbcSQLType(Long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0, new LongConverter()));
            registerjdbcSQLType(long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0, new LongConverter()));

            registerjdbcSQLType(BigDecimal.class, new TypeRef<BigDecimal>(
                            java.sql.Types.DECIMAL, "DECIMAL", 14, 8,
                            new BigDecimalConverter()));// also NUMERIC

            registerjdbcSQLType(Float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, new FloatConverter()));
            registerjdbcSQLType(float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, new FloatConverter()));

            registerjdbcSQLType(Double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE", 0, 0, new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT
            registerjdbcSQLType(double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE", 0, 0, new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT

            registerjdbcSQLType(Date.class, new TypeRef<Date>(java.sql.Types.DATE,
                            "DATE", 0, 0, true, new DateConverter()));
            registerjdbcSQLType(Time.class, new TypeRef<Time>(
                            java.sql.Types.TIME, "TIME", 0, 0, true, new TimeConverter()));
            
            registerjdbcSQLType(java.sql.Timestamp.class, new TypeRef<Timestamp>(
                            java.sql.Types.TIMESTAMP, "TIMESTAMP", 0, 0, true,
                            new TimestampConverter()));

            registerjdbcSQLType(String.class, new TypeRef<String>(
                            java.sql.Types.VARCHAR, "VARCHAR", 128, 0, true,
                            new StringConverter())); // ALSO CHAR, LONG VARCHAR

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.CLOB,
                            "CLOB", 0, 0, true, new ReaderConverter()));

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.LONGVARCHAR,
                    "LONGVARCHAR", 0, 0, true, new ReaderConverter()));

            registerjdbcSQLType(InputStream.class, new TypeRef<InputStream>(java.sql.Types.LONGVARBINARY,
                            "BLOB", 0, 0, true, new InputStreamConverter()));
            
    
    }

}