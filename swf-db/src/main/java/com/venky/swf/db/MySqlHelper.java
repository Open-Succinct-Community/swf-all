/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

import com.venky.core.util.Bucket;
import com.venky.swf.db.model.Count;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.exceptions.SWFTimeoutException;
import com.venky.swf.sql.DataManupulationStatement;
import com.venky.swf.sql.Select;

/**
 *
 * @author venky
 */
public class MySqlHelper extends JdbcTypeHelper{
    @Override
    public String getAutoIncrementInstruction() {
            return (" INTEGER NOT NULL AUTO_INCREMENT ");
    }
    @Override
    public String getCurrentTimeStampKW(){
    	return "CURRENT_TIMESTAMP";
    }
    @Override
    public String getCurrentDateKW(){
    	return "CURRENT_DATE" ;
    }
	public boolean isQueryTimeoutException(SQLException ex){
		if (!super.isQueryTimeoutException(ex)){
			return ex.getClass().getName().endsWith("TimeoutException");
		}
		return false;
	}
    public boolean hasTransactionRolledBack(Throwable ex){
    	if (super.hasTransactionRolledBack(ex) || ex instanceof SWFTimeoutException){
    		return true;
    	}
    	return false;
    }

	@Override
	public String getDefaultKW(TypeRef<?> ref,Object value) {
    	if (Boolean.class.isAssignableFrom(ref.getJavaClass()) || boolean.class.isAssignableFrom(ref.getJavaClass())) {
    		if ((Boolean)value){
    			return "b'1'";
    		}else {
    			return "b'0'";
    		}
    	}
		return super.getDefaultKW(ref, value);
	}
	
    protected MySqlHelper() {
            /**
             * Specify size and scale for a data type only if the database accepts
             * them during table creation
             */
            registerjdbcSQLType(Boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BIT", 0, 0,false,false,
                            new BooleanConverter()));
            registerjdbcSQLType(boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BIT", 0, 0,false,false,
                            new BooleanConverter()));

            registerjdbcSQLType(Byte.class, new TypeRef<Byte>(java.sql.Types.TINYINT,
                            "TINYINT", 0, 0, false,false,new ByteConverter()));

            registerjdbcSQLType(byte.class, new TypeRef<Byte>(java.sql.Types.TINYINT,
                            "TINYINT", 0, 0, false,false,new ByteConverter()));

            registerjdbcSQLType(Short.class,
                            new TypeRef<Short>(java.sql.Types.SMALLINT, "SMALLINT", 0, 0,false,false,
                                            new ShortConverter()));
            registerjdbcSQLType(short.class,
                            new TypeRef<Short>(java.sql.Types.SMALLINT, "SMALLINT", 0, 0,false,false,
                                            new ShortConverter()));

            registerjdbcSQLType(Integer.class,
                            new TypeRef<Integer>(java.sql.Types.INTEGER, "INTEGER", 0, 0,false,false,
                                            new IntegerConverter()));

            registerjdbcSQLType(int.class, new TypeRef<Integer>(java.sql.Types.INTEGER,
                            "INTEGER", 0, 0, false,false, new IntegerConverter()));

            registerjdbcSQLType(Long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0,false,false, new LongConverter()));
            registerjdbcSQLType(long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0,false,false, new LongConverter()));

            registerjdbcSQLType(BigDecimal.class, new TypeRef<BigDecimal>(
                            java.sql.Types.DECIMAL, "DECIMAL", 14, 8,false,false,
                            new BigDecimalConverter()));// also NUMERIC

            registerjdbcSQLType(Float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "FLOAT", 10, 7,false,false, new FloatConverter()));
            registerjdbcSQLType(float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "FLOAT", 10, 7, false,false,new FloatConverter()));

            registerjdbcSQLType(Double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE", 0, 0, false,false,new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT
            registerjdbcSQLType(double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE", 0, 0, false,false,new DoubleConverter())); // ALSO

            registerjdbcSQLType(Bucket.class, new TypeRef<Bucket>(
                    java.sql.Types.DOUBLE, "DOUBLE", 0, 0, false,false,new BucketConverter())); // ALSO
                                                                                                                                                            // FLOAT

            registerjdbcSQLType(Date.class, new TypeRef<Date>(java.sql.Types.DATE,
                            "DATE", 0, 0, true, true, new DateConverter()));
            registerjdbcSQLType(Time.class, new TypeRef<Time>(
                            java.sql.Types.TIME, "TIME", 0, 0, true, true ,new TimeConverter()));
            
            registerjdbcSQLType(java.sql.Timestamp.class, new TypeRef<Timestamp>(
                            java.sql.Types.TIMESTAMP, "DATETIME", 0, 0, true, false,
                            new TimestampConverter()));

            registerjdbcSQLType(String.class, new TypeRef<String>(
                            java.sql.Types.VARCHAR, "VARCHAR", 128, 0, true, true,
                            new StringConverter())); // ALSO CHAR, LONG VARCHAR

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.CLOB,
                            "LONGTEXT", 0, 0, true, true, new ReaderConverter()));

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.LONGVARCHAR,
                    "LONGTEXT", 0, 0, true, true, new ReaderConverter()));

            registerjdbcSQLType(InputStream.class, new TypeRef<InputStream>(java.sql.Types.LONGVARBINARY,
                            "LONGBLOB", 0, 0, true, true, new InputStreamConverter()));
            
    
    }

    @Override
    protected <M extends Model> void updateSequence(Table<M> table){
    	List<Count> counts = new Select("MAX(id) AS COUNT").from(table.getModelClass()).execute(Count.class);
    	Count count = counts.get(0);
    	DataManupulationStatement ddl = new DataManupulationStatement(table.getPool());
    	ddl.add("ALTER TABLE ").add(table.getRealTableName()).add(" AUTO_INCREMENT = ").add( String.valueOf(count.getCount() + 1) );
    	ddl.executeUpdate();
    }

}
