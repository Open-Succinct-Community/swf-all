/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.venky.swf.db.table;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.venky.core.collections.IgnoreCaseMap;
import com.venky.core.collections.IgnoreCaseSet;
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
            Query q = new Query();
            q.add("drop table").add(getTableName());
            q.executeUpdate();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    public void createTable() {
        try {
            Transaction txn = Database.getInstance().getCurrentTransaction();
            Query q = createTableQuery();
            q.executeUpdate();
            txn.commit();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    private Query createTableQuery(){
        Query q = new Query(); 
        q.add("create table ");
        q.add(getTableName() + "(");
        createFields(q);
        if (getReflector().getRealFields().contains("id")){
            q.add(", primary key ("+ getReflector().getColumnDescriptor("id").getName() +")");
        }
        q.add(")");
        return q;
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
    public static final String COLUMNS_DROPPED = "DROP";
    public static final String FIELDS_MODIFIED = "ALTER";
    
    public Map<String,Set<String>> getFieldsAltered(){
        Map<String,Set<String>> fieldsAltered = new IgnoreCaseMap<Set<String>>();
        fieldsAltered.put(FIELDS_ADDED, new IgnoreCaseSet());
        fieldsAltered.put(COLUMNS_DROPPED, new IgnoreCaseSet());
        fieldsAltered.put(FIELDS_MODIFIED, new IgnoreCaseSet());
        List<String> fields = reflector.getRealFields();
        List<String> columns = reflector.getRealColumns();
        Iterator<String> fieldIterator = fields.iterator();
        while( fieldIterator.hasNext() ){
            String fieldName = fieldIterator.next();
            ColumnDescriptor modelColumn = reflector.getColumnDescriptor(fieldName);
            ColumnDescriptor tableColumn = getColumnDescriptor(modelColumn.getName());
            if (tableColumn == null){
                fieldsAltered.get(FIELDS_ADDED).add(fieldName);
            }else if (!modelColumn.equals(tableColumn)){
            	System.out.println(modelColumn);
            	System.out.println(tableColumn);
            	fieldsAltered.get(FIELDS_MODIFIED).add(fieldName);
            }
        }
        for (ColumnDescriptor tableColumn : getColumnDescriptors()){ 
            if (!columns.contains(tableColumn.getName())){
                fieldsAltered.get(COLUMNS_DROPPED).add(tableColumn.getName());
            }
        }
        return fieldsAltered;
    }
    public boolean sync(){
        try {
            Map<String,Set<String>> fields = getFieldsAltered();
            Set<String> addedFields = fields.get(FIELDS_ADDED);
            Set<String> droppedColumns = fields.get(COLUMNS_DROPPED);
            Set<String> alteredFields = fields.get(FIELDS_MODIFIED);
            if (addedFields.isEmpty() && droppedColumns.isEmpty() && alteredFields.isEmpty()){
                return false;
            }
            
            Transaction txn = Database.getInstance().getCurrentTransaction();
            for (String columnName:droppedColumns){
                Query q = new Query();
                q.add( "ALTER TABLE ").add(getTableName()).add( " DROP COLUMN ").add(columnName);
                q.executeUpdate();
            }

            for (String fieldName:addedFields){
                Query q = new Query();
                q.add(" ALTER TABLE ").add(getTableName()).add( " ADD COLUMN ");
                q.add(reflector.getColumnDescriptor(fieldName).toString());
                q.executeUpdate();
            }
            
            boolean idTypeChanged = false;
            for (String fieldName:alteredFields){
            	if (fieldName.equalsIgnoreCase("ID")){
            		idTypeChanged = true;
            		continue;
            	}
            	ColumnDescriptor cd = reflector.getColumnDescriptor(fieldName);
            	String columnName = cd.getName();
            	Query q = new Query();
            	
                q.add(" ALTER TABLE ").add(getTableName()).add( " ADD COLUMN ");
                q.add("NEW_" + cd.toString());
                q.executeUpdate();
                
            	q = new Query();
            	q.add("update ").add(getTableName()).add(" SET NEW_"+columnName + " =  " + columnName);
            	q.executeUpdate();
            	
            	q = new Query();
            	q.add( "ALTER TABLE ").add(getTableName()).add( " DROP COLUMN ").add(columnName);
                q.executeUpdate();
                
                q = new Query();
                q.add(" ALTER TABLE ").add(getTableName()).add( " ADD COLUMN ");
                q.add(cd.toString());
                q.executeUpdate();
                
                q = new Query();
            	q.add("update ").add(getTableName()).add(" SET "+columnName + " =  NEW_" + columnName);
            	q.executeUpdate();
                
                q = new Query();
            	q.add( "ALTER TABLE ").add(getTableName()).add( " DROP COLUMN ").add("NEW_" + columnName);
                q.executeUpdate();
            }

            if (idTypeChanged){
            	// Rare event. Drop and recreate table.
            	Query q = new Query(); 
            	q.add("create table temp_" + getTableName()).add(" as (select * from "+ getTableName() + ")") ;
            	q.executeUpdate();
            	
                q = new Query();
                q.add("drop table ").add(getTableName());
                q.executeUpdate();
                
                q= createTableQuery();
                q.executeUpdate();
                
                q = new Query();
                q.add("insert into ").add(getTableName()).add("(");
                Iterator<String> columnIterator = reflector.getRealColumns().iterator();
                while (columnIterator.hasNext()){
                	q.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
                }
                q.add(") select ");
                columnIterator = reflector.getRealColumns().iterator();
                while (columnIterator.hasNext()){
                	q.add(columnIterator.next()).add(columnIterator.hasNext()? "," : "");
                }
                q.add(" from temp_" + getTableName());
                q.executeUpdate();
                
                q = new Query();
                q.add("drop table temp_" + getTableName());
                q.execute();
            }
            txn.commit();
            return true;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    public M get(int id) {
        Query q = new Query();
        String idColumn = getReflector().getColumnDescriptor("id").getName();
        q.select(tableName).where(idColumn).add(" = ? ", new BindVariable(id));
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

    Map<String,ColumnDescriptor> columnDescriptors = new IgnoreCaseMap<ColumnDescriptor>();
    
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
        Set<String> columns = new IgnoreCaseSet();
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
            return ((String)get("COLUMN_NAME"));
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
            return  toString().equalsIgnoreCase(othercd.toString()) ;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
}
