/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.core.string.StringUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.Database.Transaction;
import com.venky.swf.db.JdbcTypeHelper.TypeRef;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.routing.Config;




/**
 *
 * @author venky
 */
public class Table<M extends Model> {
    private final String tableName ;
    private final Class<M> modelClass;
    private final ModelReflector<M> reflector;
    public ModelReflector<M> getReflector() {
		return reflector;
	}

	private boolean existingInDatabase = false;

    public boolean isExistingInDatabase() {
        return existingInDatabase;
    }

    public void setExistingInDatabase(boolean existingInDatabase) {
        this.existingInDatabase = existingInDatabase;
    }
    
    @SuppressWarnings("unchecked")
	public Table(String tableName){
        this(tableName, (Class<M>)modelClass(tableName));
    }
    public Table(Class<M> modelClass){
        this(tableName(modelClass),modelClass);
    }
    
    private Table(String tableName, Class<M> modelClass){
        this.tableName = tableName; 
        this.modelClass = modelClass;
        if (modelClass != null){
        	this.reflector = ModelReflector.instance(modelClass);
        }else {
        	this.reflector = null;
        }
    }
    
    public static <M extends Model> String tableName(Class<M> modelClass){
        return tableName(modelClass.getSimpleName());
    }
    
    public static String tableName(String modelClassSimpleName){
        return StringUtil.underscorize(StringUtil.pluralize(modelClassSimpleName));
    }
    public static String getSimpleModelClassName(String tableName){
    	return StringUtil.camelize(StringUtil.singularize(tableName));
    }
    public static Class<?> modelClass(String tableName){
        for (String root : Config.instance().getModelPackageRoots()){
            String className = root ; 
            if (!root.endsWith(".")){
                className += "."; 
            }
            className += getSimpleModelClassName(tableName);
            
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException ex) {
                //
            }
            
        }
        return null;
    }

    public String getTableName() {
        return tableName;
    }

    public Class<M> getModelClass() {
        return modelClass;
    }
    
    public void dropTable(){
        try {
            Transaction txn = Database.getInstance().getCurrentTransaction();
            PreparedStatement ps = txn.createStatement("drop table " + getTableName());
            ps.executeUpdate();
            ps.close();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void createTable() {
        try {
            Transaction txn = Database.getInstance().getCurrentTransaction();
            Query q = new Query(); 
            q.add("create table " + tableName + "(");
            createFields(q);
            q.add(", primary key (id)");
            q.add(")");
            q.executeUpdate();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void createFields(Query q){

        List<String> fields = reflector.getRealFields();
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            Method getter = reflector.getFieldGetter(fieldName);
            ColumnDescriptor d = reflector.getColumnDescriptor(getter);
            q.add(d.toString());
            if (fieldIterator.hasNext()){
                q.add(",");
            }
        }
        
    }
    
    public static final String FIELDS_ADDED = "ADD";
    public static final String FIELDS_DROPPED = "DROP";
    public static final String FIELDS_MODIFIED = "ALTER";
    
    public Map<String,Set<String>> getFieldsAltered(){
        Map<String,Set<String>> fieldsAltered = new HashMap<String, Set<String>>();
        fieldsAltered.put(FIELDS_ADDED, new HashSet<String>());
        fieldsAltered.put(FIELDS_DROPPED, new HashSet<String>());
        fieldsAltered.put(FIELDS_MODIFIED, new HashSet<String>());
        List<String> fields = reflector.getRealFields();
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            Method getter = reflector.getFieldGetter(fieldName);
            ColumnDescriptor modelColumn = reflector.getColumnDescriptor(getter);
            ColumnDescriptor tableColumn = getColumnDescriptor(modelColumn.getName());
            if (tableColumn == null){
                fieldsAltered.get(FIELDS_ADDED).add(fieldName);
            }else if (!modelColumn.equals(tableColumn)){
                fieldsAltered.get(FIELDS_MODIFIED).add(fieldName);
            }
        }
        for (ColumnDescriptor tableColumn : getColumnDescriptors()){ 
            if (!fields.contains(tableColumn.getName())){
                fieldsAltered.get(FIELDS_DROPPED).add(tableColumn.getName());
            }
        }
        return fieldsAltered;
    }
    public boolean sync(){
        try {
            Map<String,Set<String>> fields = getFieldsAltered();
            Set<String> addedFields = fields.get(FIELDS_ADDED);
            Set<String> droppedFields = fields.get(FIELDS_DROPPED);
            Set<String> alteredFields = fields.get(FIELDS_MODIFIED);
            if (addedFields.isEmpty() && droppedFields.isEmpty() && alteredFields.isEmpty()){
                return false;
            }
            
            Transaction txn = Database.getInstance().getCurrentTransaction();
            droppedFields.addAll(alteredFields);
            for (String fieldName:droppedFields){
                Query q = new Query();
                q.add( "ALTER TABLE ").add(getTableName()).add( " DROP COLUMN ").add(fieldName);
                q.executeUpdate();
            }

            addedFields.addAll(alteredFields);
            for (String fieldName:addedFields){
                Query q = new Query();
                q.add(" ALTER TABLE ").add(getTableName()).add( " ADD COLUMN ");
                q.add(reflector.getColumnDescriptor(fieldName).toString());
                q.executeUpdate();
            }
            
            txn.commit();
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    public M get(long id) {
        Query q = new Query();
        q.select(tableName).add(" where id = ? ", new BindVariable(id));
        List<M> result = q.execute(getModelClass());
        if (result.isEmpty()){
            return null;
        }else {
            return result.get(0);
        }
    }
    public M newRecord(){
        return ModelImpl.getProxy(modelClass,new Record());
    }

    Map<String,ColumnDescriptor> columnDescriptors = new HashMap<String, ColumnDescriptor>();
    
    public Set<String> getColumnNames(){ 
        return columnDescriptors.keySet();
    }
    public Collection<ColumnDescriptor> getColumnDescriptors(){ 
        return columnDescriptors.values();
    }
    public ColumnDescriptor getColumnDescriptor(String columnName){
        return getColumnDescriptor(columnName, false);
    }
    public ColumnDescriptor getColumnDescriptor(String columnName,boolean createIfRequired){
        ColumnDescriptor c = columnDescriptors.get(columnName);
        if (c == null && createIfRequired){
            c = new ColumnDescriptor();
            columnDescriptors.put(columnName, c);
        }
        return c;
    }

    public String[] getAutoIncrementColumns(){ 
        Set<String> columns = new HashSet<String>();
        for (String name :getColumnNames()){ 
            if (getColumnDescriptor(name).isAutoIncrement()){ 
                columns.add(name);
            }
        }
        return columns.toArray(new String[]{});
    }
    public static class ColumnDescriptor extends Record{
        public ColumnDescriptor(){
            
        }
        
        public int getOrdinalPosition(){
            Integer pos = (Integer)get("ORDINAL_POSITION");
            return (pos == null? 0 : pos);
        }
        
        public String getName(){
            return ((String)get("COLUMN_NAME")).toUpperCase();
        }
        
        public int getJDBCType(){ 
            return (Integer)get("DATA_TYPE");
        }
        
        public void setName(String name){
            put("COLUMN_NAME",name);
        }
        
        public void setJDBCType(int sqlType){
            put("DATA_TYPE",sqlType);
        }

        public int getSize(){
            Integer ret = (Integer)get("COLUMN_SIZE");
            if (ret == null){
                return 0;
            }else {
                return ret;
            }
        }
        
        public void setSize(int size){
            put("COLUMN_SIZE",size);
        }
        
        public void setPrecision(int size){
            setSize(size);
        }
        
        public int getPrecision(){
            return getSize();
        }
        
        public void setScale(int scale){
            put("DECIMAL_DIGITS",scale);
        }

        public int getScale(){
            Integer retval = (Integer)get("DECIMAL_DIGITS");
            if (retval == null){
                return 0;
            }else {
                return retval;
            }
        }
        
        public boolean isNullable(){ 
            return "YES".equals(get("IS_NULLABLE"));
        }
        
        public void setNullable(boolean nullable){
            put("IS_NULLABLE",nullable ? "YES" : "NO");
        }
        
        public boolean isAutoIncrement(){ 
            return "YES".equals(get("IS_AUTOINCREMENT"));
        }
        
        public void setAutoIncrement(boolean autoincrement){
            put("IS_AUTOINCREMENT",autoincrement ? "YES": "NO");
        }
        
        private boolean virtual = false;
        
        
        public boolean isVirtual() {
			return virtual;
		}

		public void setVirtual(boolean virtual) {
			this.virtual = virtual;
		}

		@Override
        public String toString(){
            StringBuilder buff = new StringBuilder();
            TypeRef<?> ref = Database.getInstance().getJdbcTypeHelper().getTypeRef(getJDBCType());
            
            buff.append(getName()).append(" ").append(ref.getSqlType());
            if (ref.getSize() > 0 && getSize() > 0){ 
                buff.append("(").append(getSize()); 
                if (ref.getScale() > 0 && getScale() > 0){
                    buff.append(",").append(getScale());
                }    
                buff.append(")");
            }
            if (!isNullable()){
                buff.append(" NOT NULL ");
            }
            if (isAutoIncrement()){
                buff.append(Database.getInstance().getJdbcTypeHelper().getAutoIncrementInstruction());
            }
            
            return buff.toString();
        }

        @Override
        public boolean equals(Object other){
            if (other == null || !(other instanceof ColumnDescriptor)){
                return false;
            }
            ColumnDescriptor othercd = (ColumnDescriptor)other;
            return  toString().equals(othercd.toString()) ;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
}
