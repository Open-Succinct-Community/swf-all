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
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.core.date.DateUtils;
import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.StringReader;
import com.venky.core.log.TimerStatistics.Timer;
import com.venky.core.math.DoubleUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.defaulting.StandardDefaulter;

/**
 * 
 * @author venky
 */
public abstract class JdbcTypeHelper {
	public boolean isColumnNameAutoLowerCasedInDB(){
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
    
    private static int[] BLOBTYPES = new int[] {Types.BLOB , Types.LONGVARBINARY , Types.BINARY} ;
    static {
    	Arrays.sort(BLOBTYPES);
    }

    private static Class[] NUMERICTYPES = new Class[] { int.class, short.class, long.class, float.class, double.class, Number.class };
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
    public static boolean isCLOB(int jdbcType) {
    	return Arrays.binarySearch(CLOBTYPES,jdbcType)> 0;
	}
	public static boolean isBLOB(int jdbcType) {
    	return Arrays.binarySearch(BLOBTYPES,jdbcType)> 0;
	}
	public static boolean isLOB(int jdbcType){
    	return isCLOB(jdbcType) || isBLOB(jdbcType);
    }
    
    public abstract class TypeConverter<M> {

        public abstract M valueOf(Object o);
        
        public String toString(Object m) {
        	return StringUtil.valueOf(m);
        }
        
        public abstract String getDisplayClassName();
    }

    public class BooleanConverter extends TypeConverter<Boolean> {

    	public Boolean valueOf(Object s) {
            if (ObjectUtil.isVoid(s)) {
                return false;
            }
            return Boolean.valueOf(StringUtil.valueOf(s).equalsIgnoreCase("true") || StringUtil.valueOf(s).equalsIgnoreCase("1") || StringUtil.valueOf(s).equalsIgnoreCase("Y"));
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
			return "number";
		}
    }
    public abstract class NumericConverter<N extends Number> extends NumberConverter<N> {
		public String toString(Object o){
			if (o == null){
				return "";
			}
			N n = (N)o; 
			double fract = n.doubleValue() - Math.floor(n.doubleValue()); 
			DecimalFormat fmt = new DecimalFormat("##############.0000");
			if (DoubleUtils.compareTo(fract ,Math.round(fract*100.0)/100.0)<= 0){
				fmt = new DecimalFormat("##############.00");
			}
			return fmt.format(n.doubleValue());
		}
    }
    public class ShortConverter extends NumberConverter<Short> {

        public Short valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Short((short) 0);
            }
            return Short.valueOf(StringUtil.valueOf(o));
        }
        
    }

    public class IntegerConverter extends NumberConverter<Integer> {

        public Integer valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Integer(0);
            }
            return Double.valueOf(StringUtil.valueOf(o)).intValue();
        }
    }

    public class LongConverter extends NumberConverter<Long> {

        public Long valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Long(0);
            }
            return Double.valueOf(StringUtil.valueOf(o)).longValue();
        }
    }

    public class FloatConverter extends NumericConverter<Float> {

        public Float valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Float(0.0);
            }
            return Double.valueOf(StringUtil.valueOf(o)).floatValue();
        }
    }

    public class DoubleConverter extends NumericConverter<Double> {

        public Double valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Double(0.0);
            }
            return Double.valueOf(StringUtil.valueOf(o));
        }
    }

    public class BigDecimalConverter extends NumericConverter<BigDecimal> {
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
            return date == null ? "" :DateUtils.getDateStr((java.util.Date) date);
        }
        @Override
		public String getDisplayClassName() {
			return "date";
		}

    }

    public class TimeConverter extends TypeConverter<Time> {

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
            return time == null ? "" : DateUtils.getTimeStr((java.util.Date) time);
        }
        
        @Override
		public String getDisplayClassName() {
			return "time";
		}

    }

    public class TimestampConverter extends TypeConverter<Timestamp> {

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
            return ts == null ? "" : DateUtils.getTimestampStr((java.util.Date) ts);
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
            return in == null ? "" : StringUtil.read((InputStream) in);
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
            return reader == null ? "" : StringUtil.read((Reader) reader);
        }
        @Override
		public String getDisplayClassName() {
			return "string";
		}

    }
    
    private static JdbcTypeHelper _instance = null ;
    public static JdbcTypeHelper instance(Class<?> driverClass){
        if (_instance != null){
            return _instance;
        }
        synchronized(JdbcTypeHelper.class){
            if (driverClass.getName().startsWith("org.apache.derby.")){
                _instance = new DerbyHelper();
            }else if (driverClass.getName().startsWith("com.mysql")) {
                _instance = new MySqlHelper();
            }else if (driverClass.getName().startsWith("org.postgresql")) {
                _instance = new PostgresqlHelper();
            }
        }
        return _instance;
    }

    private final Map<Class<?>, TypeRef<?>> javaTypeRefMap = new HashMap<Class<?>, TypeRef<?>>();
    private final Map<Integer, List<TypeRef<?>>> jdbcTypeRefMap = new HashMap<Integer, List<TypeRef<?>>>(); 
    protected void registerjdbcSQLType(Class clazz, TypeRef ref) {
    	ref.setJavaClass(clazz);
        javaTypeRefMap.put(clazz, ref);
        List<TypeRef<?>> colTypeRefs = jdbcTypeRefMap.get(ref.jdbcType);
        if (colTypeRefs == null){
        	colTypeRefs = new ArrayList<TypeRef<?>>();
        	jdbcTypeRefMap.put(ref.jdbcType, colTypeRefs);
        }
        colTypeRefs.add(ref);
    }

    public TypeRef<?> getTypeRef(Class<?> javaClass) {
        TypeRef<?> ref = javaTypeRefMap.get(javaClass);
        if (ref != null) {
            return ref;
        }
        Timer loop = Timer.startTimer("loop:" + javaClass.getName());
    	try {
	        for (Class<?> key : javaTypeRefMap.keySet()) {
	            if (key.isAssignableFrom(javaClass)) {
	                return javaTypeRefMap.get(key);
	            }
	        }
	        return null;
    	}finally{
    		loop.stop();
    	}
    }

    public List<TypeRef<?>> getTypeRefs(int jdbcType) {
    	return jdbcTypeRefMap.get(jdbcType);
    }
    public TypeRef<?> getTypeRef(int jdbcType) {
    	List<TypeRef<?>> refs = getTypeRefs(jdbcType);
    	if (refs == null || refs.isEmpty()){
    		return null;
    	}else{
    		return refs.get(0);
    	}
    }
    
    
    
    public String getAutoIncrementInstruction() {
        return " INTEGER NOT NULL ";
    }
    
    public abstract String getCurrentTimeStampKW();
    public abstract String getCurrentDateKW();
    public String toDefaultKW(TypeRef<?> ref, COLUMN_DEF def){
    	Timer timer = Timer.startTimer();
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
    public String getDefaultKW(TypeRef ref, Object value){
    	if ( ref.isColumnDefaultQuoted() && !StringUtil.valueOf(value).startsWith("'")){
			return "'" + value + "'";
    	}else {
    		return StringUtil.valueOf(value);
    	}
    }
	
}
