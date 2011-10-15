/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.venky.core.date.DateUtils;
import com.venky.core.math.DoubleUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;

/**
 * 
 * @author venky
 */
public class JdbcTypeHelper {

    public static class TypeRef<M> {

        int jdbcType;
        String sqlType;
        int size;
        int scale;
        TypeConverter<M> typeConverter;
        Class<?> javaClass;

        public TypeRef(int jdbcType, String sqlType, int size, int scale,
                TypeConverter<M> typeConverter) {
            this.jdbcType = jdbcType;
            this.sqlType = sqlType;
            this.size = size;
            this.scale = scale;
            this.typeConverter = typeConverter;
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

    public static abstract class TypeConverter<M> {

        public abstract M valueOf(Object o);

        public String toString(Object m) {
        	return StringUtil.valueOf(m);
        }
        
        public abstract String getDisplayClassName();
    }

    public static class BooleanConverter extends TypeConverter<Boolean> {

        public Boolean valueOf(Object s) {
            if (ObjectUtil.isVoid(s)) {
                return false;
            }
            return Boolean.valueOf(StringUtil.valueOf(s).equalsIgnoreCase("true") || StringUtil.valueOf(s).equalsIgnoreCase("1"));
        }

		@Override
		public String getDisplayClassName() {
			return "boolean";
		}
    }

    public static class CharacterConverter extends TypeConverter<Character> {

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

    public static class ByteConverter extends TypeConverter<Byte> {

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

    public abstract static class NumberConverter<N extends Number> extends TypeConverter<N> {
		@Override
		public String getDisplayClassName() {
			return "number";
		}
    }
    public abstract static class NumericConverter<N extends Number> extends NumberConverter<N> {
		public String toString(Object o){
			N n = (N)o; 
			double fract = n.doubleValue() - Math.floor(n.doubleValue()); 
			DecimalFormat fmt = new DecimalFormat("##############.0000");
			if (DoubleUtils.compareTo(fract ,Math.round(fract*100.0)/100.0)<= 0){
				fmt = new DecimalFormat("##############.00");
			}
			return fmt.format(n.doubleValue());
		}
    }
    public static class ShortConverter extends NumberConverter<Short> {

        public Short valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Short((short) 0);
            }
            return Short.valueOf(StringUtil.valueOf(o));
        }
        
    }

    public static class IntegerConverter extends NumberConverter<Integer> {

        public Integer valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Integer(0);
            }
            return Integer.valueOf(StringUtil.valueOf(o));
        }
    }

    public static class LongConverter extends NumberConverter<Long> {

        public Long valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Long(0);
            }
            return Long.valueOf(StringUtil.valueOf(o));
        }
    }

    public static class FloatConverter extends NumericConverter<Float> {

        public Float valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Float(0.0);
            }
            return Float.valueOf(StringUtil.valueOf(o));
        }
    }

    public static class DoubleConverter extends NumericConverter<Double> {

        public Double valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new Double(0.0);
            }
            return Double.valueOf(StringUtil.valueOf(o));
        }
    }

    public static class BigDecimalConverter extends NumericConverter<BigDecimal> {
        public BigDecimal valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return new BigDecimal(0.0);
            }
            return new BigDecimal(StringUtil.valueOf(o));
        }
    }

    public static class StringConverter extends TypeConverter<String> {

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

    public static class DateConverter extends TypeConverter<Date> {

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
            return DateUtils.getDateStr((java.util.Date) date);
        }
        @Override
		public String getDisplayClassName() {
			return "date";
		}

    }

    public static class TimeConverter extends TypeConverter<Time> {

        public Time valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return null;
            }
            return new Time(DateUtils.getTime(StringUtil.valueOf(o)).getTime());
        }

        public String toString(Object time) {
            return DateUtils.getTimeStr((java.util.Date) time);
        }
        
        @Override
		public String getDisplayClassName() {
			return "time";
		}

    }

    public static class TimestampConverter extends TypeConverter<Timestamp> {

        public Timestamp valueOf(Object o) {
            if (ObjectUtil.isVoid(o)) {
                return null;
            }
            return new Timestamp(DateUtils.getDate(StringUtil.valueOf(o)).getTime());
        }

        public String toString(Object ts) {
            return DateUtils.getTimestampStr((java.util.Date) ts);
        }
        @Override
		public String getDisplayClassName() {
			return "timestamp";
		}

    }

    public static class InputStreamConverter extends TypeConverter<InputStream> {

        public InputStream valueOf(Object o) {
            if (o == null) {
                return new ByteArrayInputStream(new byte[]{});
            }
            if (o instanceof Blob) {
                Blob b = (Blob) o;
                try {
                    return b.getBinaryStream();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (o instanceof InputStream) {
                return (InputStream) o;
            }
            return new ByteArrayInputStream(StringUtil.valueOf(o).getBytes());
        }

        public String toString(Object in) {
            return StringUtil.read((InputStream) in);
        }
        
        @Override
		public String getDisplayClassName() {
			return "string";
		}

    }

    public static class ReaderConverter extends TypeConverter<Reader> {

        public Reader valueOf(Object o) {
            if (o == null) {
                return new StringReader("");
            }
            if (o instanceof Clob) {
                Clob b = (Clob) o;
                try {
                    return b.getCharacterStream();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            if (o instanceof Reader) {
                return (Reader) o;
            }
            return new StringReader(StringUtil.valueOf(o));
        }

        public String toString(Object reader) {
            return StringUtil.read((Reader) reader);
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
            }
        }
        return _instance;
    }

    private final Map<Class<?>, TypeRef<?>> jdbcSQLType = new HashMap<Class<?>, TypeRef<?>>();
    protected void registerjdbcSQLType(Class clazz, TypeRef ref) {
    	ref.setJavaClass(clazz);
        jdbcSQLType.put(clazz, ref);
    }

    private TypeRef<?> defaultTypeRef() {
        return jdbcSQLType.get(String.class);
    }

    public TypeRef<?> getTypeRef(Class<?> javaClass) {
        TypeRef<?> ref = jdbcSQLType.get(javaClass);
        if (ref != null) {
            return ref;
        }

        for (Class<?> key : jdbcSQLType.keySet()) {
            if (key.isAssignableFrom(javaClass)) {
                return jdbcSQLType.get(key);
            }
        }
        return null;
    }

    public List<TypeRef<?>> getTypeRefs(int jdbcType) {
    	List<TypeRef<?>> list = new ArrayList<TypeRef<?>>();
        for (TypeRef<?> ref : jdbcSQLType.values()) {
            if (ref.jdbcType == jdbcType) {
            	list.add(ref);
            }
        }
        return list;
    }
    public TypeRef<?> getTypeRef(int jdbcType) {
    	List<TypeRef<?>> refs = getTypeRefs(jdbcType);
    	if (refs.isEmpty()){
    		return null;
    	}else{
    		return refs.get(0);
    	}
    }
    
    
    
    public String getAutoIncrementInstruction() {
        return "";
    }
}
