/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;


import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.*;

import com.venky.cache.Cache;
import com.venky.core.date.DateUtils;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.StringReader;
import com.venky.core.log.SWFLogger;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.math.DoubleUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.Bucket;
import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.defaulting.StandardDefaulter;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.table.Table;
import com.venky.swf.routing.Config;

/**
 * 
 * @author venky
 */
public abstract class JdbcTypeHelper {
	public boolean supportsLimitSyntax(){
		return true;
	}
	public boolean isSavepointManagedByJdbc(){
    	return true;
    }
	
	public boolean isAutoCommitOnDDL(){
		return false;
	}
	
	public String getEstablishSavepointStatement(String name){
		return null;
	}

	public String getReleaseSavepointStatement(String name){
		return null;
	}

	public String getRollbackToSavepointStatement(String name){
		return null;
	}


	public boolean requiresSeparatePrimaryKeyClause(){
		return true;
	}

	public boolean isColumnNameAutoLowerCasedInDB(){
		return false;
	}
	
	private static Class<?> getClass(String name){
		try {
			return Class.forName(name);
		}catch (Exception ex){
			return null;
		}
	}
	private static Class<?> sqlTransactionRollbackException(){
		return getClass("java.sql.SQLTransactionRollbackException");
	}
	
	private Class<?> queryTimeoutException(){
		return getClass("java.sql.SQLTimeoutException");
	}
	public boolean hasTransactionRolledBack(Throwable ex){
		Class<?> sqlTransactionRollBackException = sqlTransactionRollbackException();
		if (sqlTransactionRollBackException == null){
			return false;
		}
		return getEmbeddedException(ex,sqlTransactionRollBackException) != null  ;
	}
	
	public boolean isQueryTimeoutSupported(){ 
		return true;
	}

	public boolean isNoWaitSupported(){
		return false;
	}
	public String getNoWaitLiteral(){
		return "";
	}
	
	public String getForUpdateLiteral(){
		return " FOR UPDATE ";
	}
	
	public boolean isQueryTimeoutException(SQLException ex){
		Class<?> queryTimeOutException = queryTimeoutException(); 
		if (queryTimeOutException != null && queryTimeOutException.isInstance(ex)){
			return true;
		}
		return false;
	}
    public static class TypeRef<M> {

        int jdbcType;
        String sqlType;
        int size;
        int scale;
        TypeConverter<M> typeConverter;
        Class<?> javaClass;
        boolean quotedWhenUnbounded;
        boolean columnDefaultQuoted;
        public TypeRef(int jdbcType, String sqlType, int size, int scale, boolean quotedWhenUnbounded, boolean columnDefaultQuoted,
                TypeConverter<M> typeConverter) {
            this.jdbcType = jdbcType;
            this.sqlType = sqlType;
            this.size = size;
            this.scale = scale;
            this.quotedWhenUnbounded = quotedWhenUnbounded;
            this.columnDefaultQuoted = columnDefaultQuoted;
            this.typeConverter = typeConverter;
        }
        public boolean isColumnDefaultQuoted() {
			return columnDefaultQuoted;
		}
        public boolean isQuotedWhenUnbounded() {
			return quotedWhenUnbounded;
		}
        
		public int getJdbcType() {
            return jdbcType;
        }

        public int getSize() {
            return size;
        }

        public String getSqlType() {
            return sqlType;
        }

        public int getScale() {
            return scale;
        }
        
        public boolean isLOB(){
        	return JdbcTypeHelper.isLOB(jdbcType);
        }
        
        public boolean isCLOB(){
        	return JdbcTypeHelper.isCLOB(jdbcType);
        }
        
        public boolean isBLOB(){
        	return JdbcTypeHelper.isBLOB(jdbcType);
        }
        
        public boolean isNumeric(){
        	return JdbcTypeHelper.isNumeric(javaClass);
        }
        public boolean isDate(){
            return JdbcTypeHelper.isDate(jdbcType);
        }
        public boolean isTimestamp(){
            return JdbcTypeHelper.isTimestamp(jdbcType);
        }

        public Class<?> getJavaClass(){
        	return javaClass;
        }
        
        public void setJavaClass(Class<?> javaClass){
        	this.javaClass = javaClass;
        }
        public TypeConverter<M> getTypeConverter() {
            return typeConverter;
        }
    }
    
    private static int[] CLOBTYPES = new int[] {Types.CLOB,Types.LONGVARCHAR} ;
    static {
    	Arrays.sort(CLOBTYPES);
    }
    
    private static int[] BLOBTYPES = new int[] {Types.BLOB , Types.LONGVARBINARY , Types.BINARY, Types.VARBINARY} ;
    static {
    	Arrays.sort(BLOBTYPES);
    }

    private static Class<?>[] NUMERICTYPES = new Class[] { int.class, short.class, long.class, float.class, double.class, Number.class };
    public static boolean isNumeric(Class<?> clazz){
    	if (clazz == null){
    		throw new NullPointerException("Trying to find if null class is numeric!");
    	}
    	for (int i = 0 ; i < NUMERICTYPES.length ; i ++ ){
    		if (NUMERICTYPES[i].isAssignableFrom(clazz)){
    			return true;
    		}
    	}
    	return false;
    }
    private static int[] DATETYPES = new int[] { Types.DATE };
    private static int[] TIMESTAMP_TYPES = new int[]  { Types.TIMESTAMP };

    public static boolean isDate(int jdbcType) {
        return Arrays.binarySearch(DATETYPES,jdbcType)>= 0;
    }
    public static boolean isTimestamp(int jdbcType) {
        return Arrays.binarySearch(TIMESTAMP_TYPES,jdbcType)>= 0;
    }

    public static boolean isCLOB(int jdbcType) {
    	return Arrays.binarySearch(CLOBTYPES,jdbcType)>= 0;
	}
	public static boolean isBLOB(int jdbcType) {
    	return Arrays.binarySearch(BLOBTYPES,jdbcType)>= 0;
	}
	public static boolean isLOB(int jdbcType){
    	return isCLOB(jdbcType) || isBLOB(jdbcType);
    }
    
    public abstract class TypeConverter<M> {

        public abstract M valueOf(Object o);
        
        public String toString(Object m) {
        	return StringUtil.valueOf(m);
        }
        public String toStringISO(Object m) { return toString(m) ; }
        
        public abstract String getDisplayClassName();
    }

    public class BooleanConverter extends TypeConverter<Boolean> {

    	public Boolean valueOf(Object s) {
            if (ObjectUtil.isVoid(s)) {
                return false;
            }
            return Boolean.valueOf(StringUtil.valueOf(s).equalsIgnoreCase("true") || StringUtil.valueOf(s).equalsIgnoreCase("1") || StringUtil.valueOf(s).equalsIgnoreCase("Y") || StringUtil.valueOf(s).equalsIgnoreCase("YES"));
        }

    	public String toString(Object m) {
    		if (m != null){
    			Boolean v = valueOf(m);
    			if (v){
    				return "Y";
    			}else {
    				return "N";
    			}
    		}
        	return StringUtil.valueOf(m);
        }

		@Override
		public String getDisplayClassName() {
			return "boolean";
		}
    }

    public class CharacterConverter extends TypeConverter<Character> {

        public Character valueOf(Object object) {
            if (ObjectUtil.isVoid(object)) {
                return new Character((char) 0);
            }
            char[] c = StringUtil.valueOf(object).toCharArray();
            if (c.length == 1) {
                return Character.valueOf(c[0]);
            }
            throw new RuntimeException("Cannot convert String "
                    + String.valueOf(object) + " to character");
        }

		@Override
		public String getDisplayClassName() {
			return "char";
		}
    }

    public class ByteConverter extends TypeConverter<Byte> {

        public Byte valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Byte((byte) 0);
            }
            return Byte.valueOf(StringUtil.valueOf(o));
        }
        @Override
		public String getDisplayClassName() {
			return "byte";
		}
    }

    public abstract class NumberConverter<N extends Number> extends TypeConverter<N> {
		@Override
		public String getDisplayClassName() {
			return "number text-right";
		}
    }
 
	@SuppressWarnings("unchecked")
	public abstract class NumericConverter<N extends Number> extends NumberConverter<N> {
		public DecimalFormat getDisplayFormat(){
			return new DecimalFormat("##############.0000");
		}
		
		public String toString(Object o){
			if (o == null){
				return "";
			}
			N n = (N)o; 
			double fract = n.doubleValue() - Math.floor(n.doubleValue()); 
			DecimalFormat fmt = getDisplayFormat();
			if (DoubleUtils.compareTo(fract ,Math.round(fract*100.0)/100.0)== 0){
				fmt = new DecimalFormat("##############.00");
			}
			return fmt.format(n.doubleValue());
		}
    }
    public class ShortConverter extends NumberConverter<Short> {
        public Short valueOf(Object o) {
        	DoubleConverter dc = (DoubleConverter) getTypeRef(Double.class).getTypeConverter();
        	return dc.valueOf(o).shortValue();
        }
        
    }

    public class IntegerConverter extends NumberConverter<Integer> {
        public Integer valueOf(Object o) {
        	DoubleConverter dc = (DoubleConverter) getTypeRef(Double.class).getTypeConverter();
        	return dc.valueOf(o).intValue();
        }
    }

    public class LongConverter extends NumberConverter<Long> {
        public Long valueOf(Object o) {
        	DoubleConverter dc = (DoubleConverter) getTypeRef(Double.class).getTypeConverter();
        	return dc.valueOf(o).longValue();
        }
    }

    public class FloatConverter extends NumericConverter<Float> {
        public Float valueOf(Object o) {
        	DoubleConverter dc = (DoubleConverter) getTypeRef(Double.class).getTypeConverter();
        	return dc.valueOf(o).floatValue();
        }
    }

    public class DoubleConverter extends NumericConverter<Double> {

        public Double valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Double(0.0);
            }else if (o instanceof Date){
            	return new Double(((Date)o).getTime());
            }else if (o instanceof Boolean){
            	BooleanConverter bc = (BooleanConverter) getTypeRef(Boolean.class).getTypeConverter();
             	return new Double( bc.valueOf("1").equals(o)? 1 : 0);
            }else {
            	return Double.valueOf(StringUtil.valueOf(o));
            }
        }
    }
    public class BucketConverter extends NumericConverter<Bucket> {

        public Bucket valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Bucket(0.0);
            }else if (o instanceof Date){
            	return new Bucket(((Date)o).getTime());
            }else if (o instanceof Boolean){
            	BooleanConverter bc = (BooleanConverter) getTypeRef(Boolean.class).getTypeConverter();
             	return new Bucket( bc.valueOf("1").equals(o)? 1 : 0);
            }else {
            	return new Bucket(Double.valueOf(StringUtil.valueOf(o)));
            }
        }
    }
    public class BigDecimalConverter extends NumericConverter<BigDecimal> {
    	public DecimalFormat getDisplayFormat(){
			return new DecimalFormat("###############.0000000000");
		}
		
        public BigDecimal valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new BigDecimal(0.0);
            }
            return new BigDecimal(StringUtil.valueOf(o));
        }
    }

    public class StringConverter extends TypeConverter<String> {

        public String valueOf(Object o) {
            return StringUtil.valueOf(o);
        }

        public String toString(Object o) {
            return StringUtil.valueOf(o);
        }
        @Override
		public String getDisplayClassName() {
			return "string";
		}
    }

    public class DateConverter extends TypeConverter<Date> {
    	private String format = null;
    	private TimeZone tz = null;
    	public DateConverter(){
    		this(DateUtils.APP_DATE_FORMAT_STR,TimeZone.getDefault());
    	}
    	public DateConverter(String format,TimeZone tz){
    		this.format = format;
    		this.tz = tz;
    	}

        public Date valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return null;
            }

            if (o instanceof java.util.Date) {
                return new Date(((java.util.Date) o).getTime());
            }

            return new Date(DateUtils.getDate(StringUtil.valueOf(o)).getTime());
        }

        public String toString(Object date) {
            return date == null ? "" : (date instanceof String ? (String)date : DateUtils.getTimestampStr(valueOf(date),tz,format));
        }
        public String toStringISO(Object date) {
            return date == null ? "" : (date instanceof String ? (String)date : DateUtils.getTimestampStr(valueOf(date),tz,DateUtils.ISO_DATE_FORMAT_STR));
        }
        @Override
		public String getDisplayClassName() {
			return "date";
		}

    }

    public class TimeConverter extends TypeConverter<Time> {
    	String format = null ;
    	TimeZone tz  = null; 
    	public TimeConverter(){
    		this(DateUtils.APP_TIME_FORMAT_STR,TimeZone.getDefault());
    	}
    	public TimeConverter(String format,TimeZone tz){
    		this.format = format;
    	}
        public Time valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return null;
            }
            if (o instanceof Time){
            	return new Time(((Time)o).getTime());
            }
            return new Time(DateUtils.getTime(StringUtil.valueOf(o)).getTime());
        }

        public String toString(Object time) {
            return time == null ? "" : (time instanceof String ? (String)time : DateUtils.getTimestampStr(valueOf(time),tz, format));
        }
        public String toStringISO(Object time) {
            return time == null ? "" : (time instanceof String ? (String)time : DateUtils.getTimestampStr(valueOf(time),tz,DateUtils.ISO_DATE_TIME_FORMAT_STR));
        }
        @Override
		public String getDisplayClassName() {
			return "time";
		}

    }

    public class TimestampConverter extends TypeConverter<Timestamp> {
    	public TimestampConverter(){
    		this(DateUtils.APP_DATE_TIME_FORMAT_STR,TimeZone.getDefault());
    	}
    	private String format;
    	private TimeZone tz;
    	public TimestampConverter(String format,TimeZone tz){
    		this.format = format;
    		this.tz = tz;
    	}
        public Timestamp valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return null;
            }
            if (o instanceof Timestamp){ 
            	return new Timestamp(((Timestamp)o).getTime());
            }
            return new Timestamp(DateUtils.getDate(StringUtil.valueOf(o)).getTime());
        }

        public String toString(Object ts) {
            return ts == null ? "" : (ts instanceof String ? (String)ts : DateUtils.getTimestampStr(valueOf(ts),tz,format));
        }

        public String toStringISO(Object ts) {
            return ts == null ? "" : (ts instanceof String ? (String)ts : DateUtils.getTimestampStr(valueOf(ts),tz,DateUtils.ISO_DATE_TIME_FORMAT_STR));
        }
        @Override
		public String getDisplayClassName() {
			return "timestamp";
		}

    }
    public class InputStreamConverter extends TypeConverter<InputStream> {

        public InputStream valueOf(Object o) {
            if (o == null) {
                return null;
            }
            if (o instanceof Blob) {
                Blob b = (Blob) o;
                try {
                    return new ByteArrayInputStream(StringUtil.readBytes(b.getBinaryStream())); 
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (o instanceof InputStream) {
            	if (o instanceof ByteArrayInputStream){
            		ByteArrayInputStream is = (ByteArrayInputStream)o;
            		is.reset();
            		return is;
            	}else {
            		return new ByteArrayInputStream(StringUtil.readBytes((InputStream)o));
            	}
            }
            if (o instanceof byte[]){
            	return new ByteArrayInputStream((byte[])o);
            			
            }
            return new ByteArrayInputStream(StringUtil.valueOf(o).getBytes());
        }

        public String toString(Object in) {
            return in == null ? "" : (in instanceof String ? (String)in : StringUtil.read(valueOf(in)));
        }
        public String toStringISO(Object in) {
            return in == null ? "" : (in instanceof String ? (String)in : Base64.getEncoder().encodeToString(StringUtil.readBytes(valueOf(in))));
        }
        @Override
		public String getDisplayClassName() {
			return "string";
		}

    }

    public class ReaderConverter extends TypeConverter<Reader> {

        public Reader valueOf(Object o) {
            if (o == null) {
                return null;
            }
            if (o instanceof Clob) {
                Clob b = (Clob) o;
                try {
                    return new StringReader(StringUtil.read(b.getCharacterStream()));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (o instanceof Reader) {
            	if (o instanceof StringReader){
            		try {
						((StringReader)o).reset();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
            		return (Reader) o;
            	}else {
            		return new StringReader(StringUtil.read((Reader)o));
            	}
            }
            return new StringReader(StringUtil.valueOf(o));
        }

        public String toString(Object reader) {
            return reader == null ? "" : (reader instanceof String ? (String)reader : StringUtil.read(valueOf(reader)));
        }
        @Override
		public String getDisplayClassName() {
			return "string";
		}

    }
    private static Cache<Class<?>,JdbcTypeHelper> _instance = new Cache<Class<?>, JdbcTypeHelper>(0,0) {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -3456286258113436179L;

		@Override
		protected JdbcTypeHelper getValue(Class<?> driverClass) {
            if (driverClass.getName().startsWith("org.apache.derby.")){
            	return new DerbyHelper();
            }else if (driverClass.getName().startsWith("com.mysql")) {
                return new MySqlHelper();
            }else if (driverClass.getName().startsWith("org.postgresql")) {
            	return new PostgresqlHelper();
            }else if (driverClass.getName().startsWith("org.sqlite")){ 
            	return new SQLiteHelper();
            }else if (driverClass.getName().startsWith("org.sqldroid")) {
            	return new SQLDroidHelper();
            }else if (driverClass.getName().startsWith("org.h2")) {
            	return new H2Helper();
            }
            return null;
		}
	};
    public static JdbcTypeHelper instance(Class<?> driverClass){
        return _instance.get(driverClass);
    }

    private final Map<Class<?>, TypeRef<?>> javaTypeRefMap = new HashMap<Class<?>, TypeRef<?>>();
    private final Map<Integer, List<TypeRef<?>>> jdbcTypeRefMap = new HashMap<Integer, List<TypeRef<?>>>(); 
    protected <T> void registerjdbcSQLType(Class<T> clazz, TypeRef<T> ref) {
    	ref.setJavaClass(clazz);
        javaTypeRefMap.put(clazz, ref);
        List<TypeRef<?>> colTypeRefs = jdbcTypeRefMap.get(ref.jdbcType);
        if (colTypeRefs == null){
        	colTypeRefs = new ArrayList<TypeRef<?>>();
        	jdbcTypeRefMap.put(ref.jdbcType, colTypeRefs);
        }
        colTypeRefs.add(ref);
    }

    public <T> TypeRef<T> getTypeRef(Class<T> javaClass) {
        TypeRef<?> ref = javaTypeRefMap.get(javaClass);
        if (ref != null) {
            return (TypeRef<T>) ref;
        }
        Timer loop = cat.startTimer("loop:" + javaClass.getName(), Config.instance().isTimerAdditive());
    	try {
	        for (Class<?> key : javaTypeRefMap.keySet()) {
	            if (key.isAssignableFrom(javaClass)) {
	            	TypeRef<?> value = javaTypeRefMap.get(key);
	            	javaTypeRefMap.put(key, value);
	                return (TypeRef<T>) value;
	            }
	        }
	        return null;
    	}finally{
    		loop.stop();
    	}
    }

    public List<TypeRef<?>> getTypeRefs(int jdbcType) {
    	List<TypeRef<?>> refs = jdbcTypeRefMap.get(jdbcType);
    	if (refs == null){
    		Config.instance().getLogger(getClass().getName()).warning("Cannot find ref for jdbcType:" + jdbcType);
    	}
    	return refs;
    }
    public TypeRef<?> getTypeRef(int jdbcType) {
    	List<TypeRef<?>> refs = getTypeRefs(jdbcType);
    	if (refs == null || refs.isEmpty()){
    		Config.instance().getLogger(getClass().getName()).warning("Cannot find ref for jdbcType:" + jdbcType);
    		return null;
    	}else{
    		return refs.get(0);
    	}
    }
    
    
    
    public String getAutoIncrementInstruction() {
        return " BIGINT NOT NULL ";
    }
    
    public abstract String getCurrentTimeStampKW();
    public abstract String getCurrentDateKW();
    public String toDefaultKW(TypeRef<?> ref, COLUMN_DEF def){
    	Timer timer = cat.startTimer(null,Config.instance().isTimerAdditive());
    	try {
	    	if (def.value() == StandardDefault.CURRENT_TIMESTAMP){
	    		return getCurrentTimeStampKW();
	    	}else if (def.value() == StandardDefault.CURRENT_DATE){
	    		return getCurrentDateKW();
	    	}else {
	    		return getDefaultKW(ref,StandardDefaulter.getDefaultValue(def));
	    	}
    	}finally{
    		timer.stop();
    	}
    }
    public String getDefaultKW(TypeRef<?> ref, Object value){
    	if ( ref.isColumnDefaultQuoted() && !StringUtil.valueOf(value).startsWith("'")){
			return "'" + value + "'";
    	}else {
    		return StringUtil.valueOf(value);
    	}
    }

  	public static Throwable getEmbeddedException(Throwable in, Class<?> instanceOfThisClass){
  		return ExceptionUtil.getEmbeddedException(in, instanceOfThisClass);
  	}
  	
  	public boolean isVoid (Object o){
  		return o == null || ObjectUtil.equals(getTypeRef(o.getClass()).getTypeConverter().valueOf(null),o);
  	}
  	
  	
  	protected <M extends Model> void updateSequence(Table<M> table){
  		Config.instance().getLogger(getClass().getName()).warning("updateSequence not implemented in " + getClass().getName() );
  	}

	public String getLowerCaseFunction() {
		return "LOWER";
	}

	public void setBinaryStream(PreparedStatement st, int i, ByteArrayInputStream in) throws SQLException {
		st.setBinaryStream(i, in, in.available());
	}
 
	private final SWFLogger cat = new SWFLogger(Config.instance().getLogger(getClass().getName()));
}
