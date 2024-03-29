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
import java.sql.Types;
import java.util.List;
import java.util.TimeZone;

import com.venky.core.date.DateUtils;
import com.venky.core.util.Bucket;
import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.model.Count;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.sql.Select;

/**
 *
 * @author venky
 */
public class PostgresqlHelper extends JdbcTypeHelper{
	@Override
	public boolean isColumnNameAutoLowerCasedInDB(){
		return true;
	}
	@Override
	public boolean isQueryTimeoutSupported(){ 
		return false;
	}
	
	public boolean isNoWaitSupported() {
		return true;
	}
	
	@Override
	public String getNoWaitLiteral(){
		return " NOWAIT ";
	}
	@Override
	public boolean isQueryTimeoutException(SQLException ex){
		if (!hasTransactionRolledBack(ex)){
			return ex.getSQLState().equals("55P03");
		}
		return super.isQueryTimeoutException(ex);
	}
	@Override
	public boolean hasTransactionRolledBack(Throwable ex){
		if (super.hasTransactionRolledBack(ex)){
			return true;
		}
		SQLException sqlex = (SQLException) ExceptionUtil.getEmbeddedException(ex,SQLException.class);
		if (sqlex != null && (sqlex.getSQLState().startsWith("08") || sqlex.getSQLState().startsWith("57"))){
			return true;
		}
		return false;
	}
    @Override
    public String getAutoIncrementInstruction() {
            return (" BIGSERIAL ");
    }
    
    @Override
    public String getCurrentTimeStampKW(){
    	return "now()";
    }
    public String getCurrentDateKW(){
    	return "('now'::text)::date";
    }

    @Override
    public String getDefaultKW(TypeRef<?> ref,Object value){
    	if (String.class.isAssignableFrom(ref.getJavaClass())){
    		return "'" + value + "'::character varying";
    	}
    	return super.getDefaultKW(ref,value);
    }

    
    protected PostgresqlHelper() {
            /**
             * Specify size and scale for a data type only if the database accepts
             * them during table creation
             */
            registerjdbcSQLType(Boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BOOLEAN", 0, 0,false,false,
                            new BooleanConverter()));
            registerjdbcSQLType(boolean.class, new TypeRef<Boolean>(
                            java.sql.Types.BIT, "BOOLEAN", 0, 0,false,false,
                            new BooleanConverter()));

            registerjdbcSQLType(Byte.class, new TypeRef<Byte>(java.sql.Types.SMALLINT,
                            "SMALLINT", 0, 0, false,false,new ByteConverter()));

            registerjdbcSQLType(byte.class, new TypeRef<Byte>(java.sql.Types.SMALLINT,
                            "SMALLINT", 0, 0, false,false,new ByteConverter()));

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
                            "INTEGER", 0, 0, false,false,new IntegerConverter()));

            registerjdbcSQLType(Long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0, false,false,new LongConverter()));
            registerjdbcSQLType(long.class, new TypeRef<Long>(java.sql.Types.BIGINT,
                            "BIGINT", 0, 0, false,false,new LongConverter()));

            registerjdbcSQLType(BigDecimal.class, new TypeRef<BigDecimal>(
                            java.sql.Types.DECIMAL, "DECIMAL", 14, 8,false,false,
                            new BigDecimalConverter()));// also NUMERIC
            registerjdbcSQLType(BigDecimal.class, new TypeRef<BigDecimal>(
                    java.sql.Types.NUMERIC, "DECIMAL", 14, 8,false,false,
                    new BigDecimalConverter()));// also NUMERIC

            registerjdbcSQLType(Float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, false,false,new FloatConverter()));
            registerjdbcSQLType(float.class, new TypeRef<Float>(java.sql.Types.REAL,
                            "REAL", 0, 0, false,false,new FloatConverter()));

            registerjdbcSQLType(Double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE PRECISION", 0, 0, false,false,new DoubleConverter())); // ALSO
                                                                                                                                                            // FLOAT
            registerjdbcSQLType(double.class, new TypeRef<Double>(
                            java.sql.Types.DOUBLE, "DOUBLE PRECISION", 0, 0, false,false,new DoubleConverter())); // ALSO
    
            registerjdbcSQLType(Bucket.class, new TypeRef<Bucket>(
                    		java.sql.Types.DOUBLE, "DOUBLE PRECISION", 0, 0, false,false,new BucketConverter())); // ALSO                           // FLOAT

            registerjdbcSQLType(Date.class, new TypeRef<Date>(java.sql.Types.DATE,
                            "DATE", 0, 0, true ,false, new DateConverter(DateUtils.ISO_DATE_FORMAT_STR,TimeZone.getDefault())));
            registerjdbcSQLType(Time.class, new TypeRef<Time>(
                            java.sql.Types.TIME, "TIME", 0, 0, true, false, new TimeConverter(DateUtils.ISO_TIME_FORMAT_STR, TimeZone.getDefault())));
            
            registerjdbcSQLType(java.sql.Timestamp.class, new TypeRef<Timestamp>(
                            java.sql.Types.TIMESTAMP, "TIMESTAMP", 0, 0, true, false, 
                            new TimestampConverter()));

            registerjdbcSQLType(String.class, new TypeRef<String>(
                            java.sql.Types.VARCHAR, "VARCHAR", 128, 0, true,true, 
                            new StringConverter())); // ALSO CHAR, LONG VARCHAR

            registerjdbcSQLType(Reader.class, new TypeRef<Reader>(java.sql.Types.VARCHAR,
                            "VARCHAR",10485760 , 0, true, true, new ReaderConverter()));

            registerjdbcSQLType(InputStream.class, new TypeRef<InputStream>(java.sql.Types.BINARY,
                            "BYTEA", 0, 0, true, true, new InputStreamConverter()));
            
            registerjdbcSQLType(InputStream.class, new TypeRef<InputStream>(java.sql.Types.BLOB,
                            "BYTEA", 0, 0, true, true,  new InputStreamConverter()));

			registerjdbcSQLType(String[].class, new TypeRef<>(Types.ARRAY,
				"TEXT ARRAY",0,0,true,true,new StringArrayConverter()));

	}
    @Override
    protected <M extends Model> void updateSequence(Table<M> table){
    	List<Count> counts = new Select("MAX(id) AS COUNT").from(table.getModelClass()).execute(Count.class);
    	Count count = counts.get(0);
    	if (count.getCount() < getPrimaryKeyOffset() ){
    		count.setCount(getPrimaryKeyOffset() - 1L);
		}
    	Select updateSequence = new Select("setval('"+table.getTableName()+"_id_seq',"+ (count.getCount() + 1L) +") AS COUNT").from(new String[]{});
    	updateSequence.addPool(table.getPool());
    	updateSequence.execute(Count.class);
    }
}
